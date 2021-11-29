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
    private static final String TARGETPATH = BASEDIR + "target/test-classes/Test.class";

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
        return builder.makeCallGraph(options, null);
    }

    private HashMap<String, List> createDefUseMatrix(String targetClass) throws IOException, WalaException, IllegalArgumentException, InvalidClassFileException, com.ibm.wala.util.CancelException {
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
                    int n = instructions.length;
                    HashMap<Integer, HashSet<Integer>> useMap = new HashMap<>();
                    // 对于每一条IR指令，我们获取相应的def-use对，此时变量还是以虚拟编号的形式存在
                    for (SSAInstruction ssaInstruction : instructions) {
                        if (ssaInstruction == null) continue;
                        String instruction = ssaInstruction.toString();
                        instruction += ' ';
                        // 记录指令中的所有数字（即变量的编号）
                        ArrayList<Integer> nums = new ArrayList<>();
                        int num = 0;
                        for (int j = 0; j < instruction.length(); ++j) {
                            char ch = instruction.charAt(j);
                            if (ch == '@') break;
                            if (ch == '<') {
                                while (instruction.charAt(j) != '>') ++j;
                                continue;
                            }
                            if (ch >= '0' && ch <= '9') num = num * 10 + (ch - '0');
                            else {
                                if (num == 0) continue;
                                nums.add(num);
                                num = 0;
                            }
                        }
                        // 由于第一个出现的变量编号为定义的变量，后续的变量编号均为使用的变量，我们将其改成键值对的形式
                        if (nums.size() == 0) continue;
                        if (!instruction.startsWith(nums.get(0).toString())) continue;
                        int def = nums.get(0);
                        for (int j = 1; j < nums.size(); ++j) {
                            useMap.computeIfAbsent(def, k -> new HashSet<>());
                            useMap.get(def).add(nums.get(j));
                        }
                    }

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
                    for (int i = 0; i < cnt; ++i) dus[i][i] = 1;
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
                    result.put(method.toString(), new ArrayList());
                    result.get(method.toString()).add(vars);
                    result.get(method.toString()).add(dus);
                }
            }
        }
        return result;
    }

    private int[][] multiply(int[][] a, int[][] b) {
        int m = a.length, r = b.length, n = b[0].length;
        int[][] result = new int[m][n];
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (result[i][j] == 1) continue;
                for (int k = 0; k < r; ++k) {
                    if (a[i][k] == 1 && b[k][j] == 1) {
                        result[i][j] = 1;
                        break;
                    }
                }
            }
        }
        return result;
    }

    // 快速幂求矩阵闭包
    private int[][] calculateClosure(int[][] matrix) {
        int n = matrix.length;
        int[][] result = new int[n][n];
        for (int i = 0; i < n; ++i) result[i][i] = 1;
        while (n > 0) {
            if (n % 2 == 1) result = multiply(result, matrix);
            else matrix = multiply(matrix, matrix);
            n >>= 1;
        }
        return result;
    }

    private String[] selectOracleData(String targetClass, String targetMethod, int m, int alpha, int choice) throws WalaException, CancelException, InvalidClassFileException, IOException {
        // alpha用于确定路径中FOC传递的程度
        // choice用于选择fp的值，0表示每个oracle数据具有动态的fp值，1表示使用统一的fp = 1
        HashMap<String, List> result = createDefUseMatrix(targetClass);
        String targetKey = "";
        for (String key : result.keySet()) {
            if (key.contains(targetMethod)) targetKey = key;
        }
        String[] vars = (String[])result.get(targetKey).get(0);
        int n = vars.length;
        int[][] dus = (int[][])result.get(targetKey).get(1);

        // 初始状态下所有已选oracle数据集为空，所有oracle数据的FOC均为0
        boolean[] selected = new boolean[n];
        int[][] foc;
        if (alpha == 1) foc = calculateClosure(dus);
        else foc = dus;
        int[] FOC = new int[n];
        int[] fp = new int[n];
        int FP = 1;
        // 计算所有oracle数据的FOC值
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (foc[i][j] > 0) {
                    FOC[i] += foc[i][j];
                }
            }
        }
        // 选出故障检测能力最大的m个oracle数据集
        String[] data = new String[m];
        for (int i = 0; i < m; ++i) {
            int maxFOC = 0, idx = -1;
            // 挑选出未被选择数据中FOC值最大的oracle数据idx
            for (int j = 0; j < n; ++j) {
                if (selected[j]) continue;
                if (FOC[j] > maxFOC) {
                    maxFOC = FOC[j];
                    idx = j;
                }
            }
            // idx == -1意味着之前已选择的oracle数据能够覆盖到所有的变量
            if (idx == -1) {
                data[i] = null;
                continue;
            }
            selected[idx] = true;
            FOC[idx] = 0;
            // 对于数据idx所能检测到故障的数据W，消除W对于其他数据FOC值的影响
            // fp测量选定oracle数据对未选定oracle数据的影响
            for (int j = 0; j < n; ++j) {
                if (foc[idx][j] == 0) continue;
                if (choice == 0) {
                    fp[j] += foc[idx][j];
                    FP = fp[j];
                }
                for (int s = 0; s < n; ++s) {
                    if (idx != s && foc[s][j] > 0 && !selected[s]) {
                        FOC[s] -= foc[s][j] * FP;
                        foc[s][j] -= foc[s][j] * FP;
                    }
                }
            }
            data[i] = vars[idx];
        }
        return data;
    }

    public static void main(String[] args) throws IOException, WalaException, IllegalArgumentException, InvalidClassFileException, ClassNotFoundException, com.ibm.wala.util.CancelException {
        MYSODS mysods = new MYSODS();
        System.out.println(Arrays.toString(mysods.selectOracleData(TARGETPATH, "Test1", 3, 0, 1)));
        System.out.println(Arrays.toString(mysods.selectOracleData(TARGETPATH, "Test1", 3, 1, 1)));

        System.out.println(Arrays.toString(mysods.selectOracleData(TARGETPATH, "Test2", 3, 0, 1)));
        System.out.println(Arrays.toString(mysods.selectOracleData(TARGETPATH, "Test2", 3, 1, 1)));

        System.out.println(Arrays.toString(mysods.selectOracleData(TARGETPATH, "Test3", 3, 0, 1)));
        System.out.println(Arrays.toString(mysods.selectOracleData(TARGETPATH, "Test3", 3, 1, 1)));
    }
}
