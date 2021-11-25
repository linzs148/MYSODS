# Oracle选择工具SODS复现

## 复现重点

- 概率替换图的构建
- 预言数据权重的生成
- 预言数据的选择算法



## 什么是测试预言？

> 用于确定程序是否按照给定测试输入的预期执行，它直观地由测试期间要观察的变量及其预期值组成。由于程序通常由各种内部变量和输出变量组成，在软件测试的不同位置可以观察到这些变量，因此该程序有许多候选oracle数据要包含在测试oracle中。
>
> 测试oracle包含的oracle数据越多，测试oracle检测故障的能力就越强。但是使用全部或大部分oracle数据构建测oracle的成本可能非常高，因为开发人员需要指定这些变量的预期值。因此，出现了oracle数据选择问题，目的是在构建测试oracle时减少oracle数据的数量。
>
> 测试预言验证程序是否按预期运行的机制
>
> 选择正确的oracle数据进行观察是构建测试预言的关键



## oracle数据选择的动态方法

测试执行信息

* 变量值
* 交互信息

不足：收集动态执行信息可能会产生额外的成本



##  SODS (Static Oracle Data Selection)静态预言数据选择

> 通过基于被测程序的定义-使用链来构建概率替换图以识别候选oracle数据之间的替换关系，然后估计每个候选oracle数据的故障观察能力，最后选择具有较强故障观察能力的oracle数据子集。

Basic SODS

* 通过基于被测程序的定义-使用链来构建概率替换图，概率替换图是一个表示候选oracle数据在多大程度上可以替代其他数据的图；
* 通过考虑替换关系的转移程度(以α度量)估计每个候选oracle数据在观察每个语句中的错误方面的能力；
* 根据故障观察能力和选定oracle数据的影响(通过FP测量)确定候选oracle数据的选择顺序。

Extended SODS

* Basic SODS；
* 基于0-1-CF对测试调用图分析来裁剪被测程序，以提高oracle数据选择的有效性。(只关注程序中被测试的部分)



## oracle数据

在变量涉及**赋值、函数传参、函数返回值**的时候定义对应的候选oracle数据

~~为了减少候选Oracle数据的数量，只考虑变量**第一次**赋值的时候定义对应的候选oracle数据~~



example

```C++
void factorial() {
    int n, factorial, i;
    read("Enter the number:", n);//o1
    factorial = 1;//o2
    i = n;//o3
    if (n < 0) print("wrong input is:", n);
    else {
    	if (n == 0) factorial = 0;//o4
        else {
            while (i > 0) {
                factorial = multiply(factorial, i);//o5
                i = i−1;//o6
            }
            print("The result is:", factorial);
        }

int multiply(int num1, int num2) {//o7, o8
    int result;
	return result = num1 ∗ num2;//o9
}
```



定义-使用链: 两个oracle数据$o_i$和$o_j$组成的结构，以确保**变量$o_i$的定义使用$o_j$定义的变量的值**而无需其他oracle数据干预。符号表示为$o_i <= o_j$

例如，$o_3$和$o_1$构成一个定义使用链，观察$o_3$可以检测到$o_1$的值错误导致的故障。因此在软件测试中$o_3$可以作为$o_1$的替代品。

注意定义-使用链具有**传递性**



## oracle数据选择

* 由于使用所有候选oracle数据构建测试oracle的成本很高，因此有必要确定这些候选oracle数据的选择顺序，以便开发人员可以使用少量oracle数据构建高质量的测试oracle；
* 直观地说，在构建测试oracle时，具有较大故障观察能力的候选oracle数据往往会提前选择；
* 但是，仅根据oracle数据的故障检测能力排序来选择oracle数据可能不太有效，因为某些候选oracle数据会观察由相同语句导致的故障。

oracle数据选择的目标应该是**最大化选定oracle数据集的故障观察能力**，而不是最大化每个选定oracle数据单独的故障观察能力。



**PSG权重分配**

* 所有循环将被视为有一次迭代，$while (i > 0) => if (i > 0)$;

* $o_i$和$o_j定义的语句总是一起执行 => $P(o_i > o_j) = 1$;
* 对if语句应用乘法原理，例如$P(o_6 > o_3) = \overline{b_1} * \overline{b_2} * b_3$， $b_1$、$b_2$和$b_3$分别表示$n < 0$、$n == 0$和$i > 0$的概率



## 估计故障观测能力

$W(o_i)$: 遍历PSG以查找通过$o_i$可以检测到故障的所有oracle数据集合(包括$o_i$本身)(数据？语句？)

$FOC(i, j)$: $o_i$检测$o_j$导致的故障的能力大小



FOC计算

* $FOC(i, i) = 1$
* $o_i <= o_j => FOC(i, j) = P(o_i > o_j)$
* $FOC(path) = α ^ {s - 1} * P(o_i, o_{k1}) * ... * P(o_{ks - 1}, o_{ks})$, $α$用于测量替代关系的程度
* $FOC(i, j) = \sum^l_{t = 1}{path_t}$

![](C:\Users\linzs148\Desktop\工具\images\graph.png)

* $FOC(i) = \sum^{}_{o_j \in W(o_i)}{FOC(i, j)}$



## 基于启发式算法的oracle数据选择

(考虑重复覆盖造成的影响，类似于增量贪心算法)

伪代码

```pseudocode
// Algorithm 1 Oracle data selection
// 初始状态下所有已选oracle数据集为空，所有oracle数据的FOC均为0
for each i (1 <= i <= n) do
	Selected[i] = false
	FOC[i] = 0
	// fp[i] = 0
end for

// 计算所有oracle数据的FOC值
for each i (1 <= i <= n) do
	for each j (1 <= j <= n) do
		if FOC[i, j] > 0 then
			FOC[i] = FOC[i] + FOC[i, j]
		end if
	end for
end for

// 选出故障检测能力最大的m个oracle数据集
for each j (1 <= j <= m) do
	current = 0
	k = 0
	// 挑选出未被选择数据中FOC值最大的oracle数据k
	for each i (1 <= i <= n) do
		if not Selected[i] then
			if FOC[i] > current then
				current = FOC[i]
				k = i
			end if
		end if
	end for
	Selected[k] = true
	FOC[k] = 0
	// 对于数据k所能检测到故障的数据W(o_k)，消除W(o_k)对于其他数据FOC值的影响
	// fp测量选定oracle数据对未选定oracle数据的影响
	for each i (1 <= i <= n) do
		if FOC[k, i] > 0 then
			// fp[i] = fp[i] + FOC[k, i]
			// fp = fp[i]
            for each s (1 <= s <= n) do
                if k != s ^ FOC[s, i] > 0 ^ not Selected[s] then
                    FOC[s] = FOC[s] - FOC[s, i] * fp
                    FOC[s, i] = FOC[s, i] - FOC[s, i] * fp
                end if
            end for
        end if
    end for
end for
```



## $f_p$的选择

* 乐观的角度

  * 由于选择$o_k$有助于观察所有$FOC[K，i] ＞ 0$的语句i中的错误，所以不需要再去观察这些语句，将$f_p$的值设置为1

* $f_p$数组

  * 考虑到选定的oracle数据对不同的未选定oracle数据有不同的影响，为不同的oracle数据提供了不同的$f_p$值

  * 也就是说我们使用数组$fp[i] (i <= i <= n)$表示$o_i$的$f_p$值

  * 初始状态下所有oracle数据的$p_f$值均为0

  * 一旦选择了$o_k$，对于$FOC[k, i] > 0$的那些语句i我们更新$f_p[i] = f_p[i] + FOC[k, i]$

  * 这意味着选择了$o_k$会增加对未选定oracle数据的故障观察能力的影响

  * 对于$o_i$，选择到的能够检测到$o_i$错误的语句越多，$f_p[i]$的值也就相应越大，$FOC[s，i]$的值就越小

  * 换句话说，我们通过增加$o_i$的$f_p$值使得算法倾向于不选择哪些$FOC[s，i] > 0$的oracle数据

  * 因为这些故障可能已经被足够多已选定的oracle数据检测到



## 程序剪裁

* 在实践中测试通常是不充分的，因此不必考虑所有候选oracle数据之间的替换关系
* 对于任何具有可分析代码段形式的测试的程序(例如JUnit测试)，我们进一步扩展了静态技术，以基于静态调用图分析定制程序



## 实验

关注问题

* 不同的配置(即$α$和$f_p$)如何影响SODS的有效性?
* SODS在有效性和效率方面与现有的动态方法相比如何?
* 选定oracle数据的数量(m)如何影响SODS的有效性？



使用的工具

* C主题的静态分析工具Crystal
* Java主题的分析工具WALA



自变量

* oracle数据选择方法
  * SODS
  * MAODS
  * DODONA
* SODS配置
  * 用于测量替代关系的程度的$α$(0, 0.25, 0.5, 1)
  * 选定的oracle数据对不同的未选定oracle数据有不同的影响$f_p$
* 选定oracle数据的大小(1~10)



因变量

* oracle数据集故障检测率(有效性)
* oracle数据集选择的总时间(效率)



实验过程

1. 记录oracle数据选择总时间
2. 在原始程序执行期间进行代码插装记录oracle数据的实际值来获得相应测试输入的预期值，在代码分析过程中检查故障程序上对应oracle数据的实际值是否与相应的预期值相同，以此来判断是否检测到相应的故障
3. 计算每个方法选择的oracle数据集在20个不同突变组（即800个故障）上检测到的故障率并进行比较



过程简化

* 假设条件语句中的每个分支都具有相同的执行概率
* 不处理变量的别名
* 在构建候选oracle数据集时，类的每个数组变量、对象变量、堆变量或成员变量都被视为单个变量





## 复现过程

下载wala JAR包

导包

写exclusions.txt和scope.txt



下载graphviz

### 问题

> Exception in thread "main" com.ibm.wala.ipa.cha.ClassHierarchyException: failed to load root <Primordial,Ljava/lang/Object> of class hierarchy

修改wala.properties中的java_runtime_dir为项目的jdk地址

















我们计划通过进一步优化来改进我们的基本技术，例如正交列表表示[35]和Johnson算法[44]，其目标是存储和操作稀疏矩阵。



https://www.cs.cornell.edu/projects/crystal/

Crystal 是一个用 Java 编写的 c 语言程序分析系统。该系统旨在通过提供程序的简单规范表示、标准程序分析(如指针分析)和支持整个程序分析，使编写程序分析更加容易。Java 使得系统是可移植的，并且允许分析编写器从现有的 IDE 和其他 Java 软件开发工具中获益。当前的表现形式、分析和特征包括:

- 该项目的原始抽象语法树;
- 控制流图形式的简化程序表示，其中语句和表达式以规范形式表示;
- 通过基于统一的、字段敏感的指针分析进行内存分区;
- 在分析多个文件时合并全局符号表

Crystal 目前支持 ISO C99标准和几个常见的 GNU c 扩展。



https://github.com/wala/WALA/blob/master/README-Gradle.md#intellij-idea

https://blog.csdn.net/weixin_44712778/article/details/113105671

https://blog.csdn.net/xiyi5609/article/details/78779574

https://github.com/SCanDroid/SCanDroid

https://github.com/Athithyaa/ConstantPropagationWala/blob/master/primordial.jar.model
