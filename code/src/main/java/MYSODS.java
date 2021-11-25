import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.examples.drivers.ScopeFileCallGraph;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MYSODS {
    private static final String BASEDIR = "C:/Users/linzs148/Desktop/工具/MYSODS/code/";
    private static final String SCOPEPATH = BASEDIR + "src/main/resources/scope.txt";
    private static final String EXCLUSIONSPATH = BASEDIR + "src/main/resources/exclusions.txt";
    private static final String TARGETPATH = BASEDIR + "target/test-classes/Test1.class";

    private CallGraph createCallGraph(String targetClass) throws CallGraphBuilderCancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        // 确定分析域和需要排除的Java内置类
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(
                SCOPEPATH,
                new File(EXCLUSIONSPATH),
                ScopeFileCallGraph.class.getClassLoader()
        );
        // 将需要进行分析的class文件加到分析域中
        scope.addClassFileToScope(ClassLoaderReference.Application, new File(targetClass));
        // 生成类层次关系
        ClassHierarchy hierarchy = ClassHierarchyFactory.makeWithRoot(scope);
        // 构建调用关系图
        AllApplicationEntrypoints entrypoints = new AllApplicationEntrypoints(scope, hierarchy);
        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
        SSAPropagationCallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), hierarchy, scope);
        CallGraph graph = builder.makeCallGraph(options, null);
        return graph;
    }

    public HashMap<String, List> createDefUseMatrix(String targetClass) throws IOException, WalaException, IllegalArgumentException, InvalidClassFileException, com.ibm.wala.util.CancelException {
        // 返回值为每个函数对应的变量名数组以及变量之间的调用关系矩阵
        CallGraph graph = createCallGraph(targetClass);
        HashMap<String, List> result = new HashMap<>();
        for (CGNode node : graph) {
            // 自己定义的类的方法都是ShrikeBTMethod对象
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                // 我们不关心使用Primordial类加载器加载的Java原生类
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    IR ir = node.getIR();
                    SSAInstruction[] instructions = ir.getInstructions();
                    DefUse du = node.getDU();
                    HashMap<Integer, HashSet<Integer>> useMap = new HashMap<>();
                    // 对于每一条IR指令，我们获取相应的def-use对，此时变量还是以虚拟编号的形式存在
                    for (SSAInstruction ssaInstruction : instructions) {
                        if (ssaInstruction == null) continue;
                        String instruction = ssaInstruction.toString();
                        // 如果IR指令的开头不是变量的编号，则说明该条指令没有定义变量
                        if (instruction.charAt(0) < '0' || instruction.charAt(0) > '9') continue;
                        instruction += ' ';
                        // 记录指令中的所有数字（即变量的编号）
                        ArrayList<Integer> nums = new ArrayList<>();
                        int num = 0;
                        for (int j = 0; j < instruction.length(); ++j) {
                            char ch = instruction.charAt(j);
                            if (ch >= '0' && ch <= '9') num = num * 10 + (ch - '0');
                            else {
                                // 指令在@符号后面的部分显示的是对应源代码中的行号以及该条指令定义的异常编号，我们不关心
                                if (ch == '@') break;
                                else if (ch == ' ' || ch == ',') {
                                    if (num != 0) nums.add(num);
                                    num = 0;
                                }
                            }
                        }
                        // 由于第一个出现的变量编号为定义的变量，后续的变量编号均为使用的变量，我们将其改成键值对的形式
                        int def = -1;
                        for (int j = 0; j < nums.size(); ++j) {
                            if (j == 0) def = nums.get(0);
                            else {
                                useMap.computeIfAbsent(def, k -> new HashSet<Integer>());
                                useMap.get(def).add(nums.get(j));
                            }
                        }
                    }
                    int n = instructions.length;
                    // System.out.println(useMap);
                    // 我们对所有的有名字的变量的编号进行重新排序
                    HashMap<Integer, Integer> varIndexMap = new HashMap<>();
                    int cnt = 0;
                    // 对于上面得到的useMap键值对里面所有的变量编号，如果能通过ir.getLocalNames()方法获取名字，就将其加入varIndexMap中，并自增cnt编号
                    for (int key : useMap.keySet()) {
                        if (!varIndexMap.containsKey(key)) {
                            String[] names = ir.getLocalNames(n - 1, key);
                            if (names != null) varIndexMap.put(key, cnt++);
                        }
                        for (int val : useMap.get(key)) {
                            if (!varIndexMap.containsKey(val)) {
                                String[] names = ir.getLocalNames(n - 1, val);
                                if (names != null) varIndexMap.put(val, cnt++);
                            }
                        }
                    }
                    // System.out.println(varIndexMap);
                    // vars记录按照上面编号排序的变量名字
                    // dus记录各个有名字的变量之间的def-use关系
                    String[] vars = new String[cnt];
                    int[][] dus = new int[cnt][cnt];
                    // 对于useMap中匿名的中间变量，我们需要利用其传递性得到与其相关联的有名字的变量之间的关系
                    // 举例来说，对于语句 y = f(x) + 1，生成的IR指令为 _ = f(x) 和 y = _ + 1，我们利用匿名的 _ 与 x、y 的关系以及传递性得到y使用了x
                    for (int key : useMap.keySet()) {
                        if (varIndexMap.containsKey(key)) continue;
                        HashSet<Integer> uses = useMap.get(key);
                        for (int pre : useMap.keySet()) {
                            if (useMap.get(pre).contains(key)) {
                                useMap.get(pre).addAll(uses);
                            }
                        }
                    }
                    // 对于所有的所有名字的变量，用dus矩阵记录彼此之间的def-use关系
                    // dus[x][y] = 1 表明x变量的定义使用了y变量
                    for (int key : varIndexMap.keySet()) {
                        int val = varIndexMap.get(key);
                        vars[val] = ir.getLocalNames(n - 1, key)[0];
                        if (!useMap.containsKey(key)) continue;
                        for (int use : useMap.get(key)) {
                            if (varIndexMap.containsKey(use)) {
                                dus[val][varIndexMap.get(use)] = 1;
                            }
                        }
                    }
                    // System.out.println(Arrays.toString(vars));
                    // System.out.println(Arrays.deepToString(dus));
                    result.put(method.toString(), new ArrayList());
                    result.get(method.toString()).add(vars);
                    result.get(method.toString()).add(dus);
                }
            }
        }
        return result;
    }

    private String[] selectOracleData(String targetClass, String targetMethod, int m) throws WalaException, CancelException, InvalidClassFileException, IOException {
        HashMap<String, List> result = createDefUseMatrix(targetClass);
        String targetKey = "";
        for (String key : result.keySet()) {
            if (key.contains(targetMethod)) targetKey = key;
        }
        String[] vars = (String[])result.get(targetKey).get(0);
        int[][] dus = (int[][])result.get(targetKey).get(1);
        int n = vars.length;

        // 初始状态下所有已选oracle数据集为空，所有oracle数据的FOC均为0
        boolean[] selected = new boolean[n];
        int[][] foc = new int[n][n];
        int[] focs = new int[n];
        // int[] fp = new int[n];
        int fp = 0;
        for (int i = 0; i < n; ++i) {
            selected[i] = false;
            focs[i] = 0;
            // fp[i] = 0;
        }
        // 计算所有oracle数据的FOC值
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (foc[i][j] > 0) {
                    focs[i] += foc[i][j];
                }
            }
        }
        // 选出故障检测能力最大的m个oracle数据集
        for (int j = 0; j < m; ++j) {
            int current = 0, k = 0;
            // 挑选出未被选择数据中FOC值最大的oracle数据k
            for (int i = 0; i < n; ++i) {
                if (selected[i]) continue;
                if (focs[i] > current) {
                    current = focs[i];
                    k = i;
                }
            }
            selected[k] = true;
            focs[k] = 0;
            // 对于数据k所能检测到故障的数据W(o_k)，消除W(o_k)对于其他数据FOC值的影响
            // fp测量选定oracle数据对未选定oracle数据的影响
            for (int i = 0; i < n; ++i) {
                if (foc[k][i] == 0) continue;
                // fp[i] = fp[i] + FOC[k, i]
                // fp = fp[i]
                for (int s = 0; s < n; ++s) {
                    if (k != s && foc[s][i] > 0 && !selected[s]) {
                        focs[s] -= foc[s][i] * fp;
                        foc[s][i] -= foc[s][i] * fp;
                    }
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException, WalaException, IllegalArgumentException, InvalidClassFileException, ClassNotFoundException, com.ibm.wala.util.CancelException {
        MYSODS mysods = new MYSODS();
        // String[] data = mysods.selectOracleData(TARGETPATH, "main", 3);
        HashMap<String, List> data = mysods.createDefUseMatrix(TARGETPATH);
        System.out.println(data);
    }
}
