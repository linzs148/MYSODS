# Oracle选择工具SODS复现

[Supporting Oracle Construction Via Static Analysis](https://github.com/linzs148/MYSODS/blob/main/resources/%5BASE'16%5DSupporting_oracle_construction_via_static_analysis.pdf)

**（由于论文没有提供源码，WALA的使用也没什么教程，更没有系统性学过程序分析，所以复现的程度十分有限，希望助教能够多给点分呜呜呜）**

## 论文理解

从宏观的角度来看，错误是由开发人员在编程中的错误引起的，错误的语句表明了这一点。从微观角度看，错误在执行过程中由变量的值反映出来。因此，观察oracle数据可能会检测到错误语句导致的故障。

故障的传播 PIE模型

### 什么是测试预言？



> 由于程序通常由各种内部变量和输出变量组成，在软件测试的不同位置可以观察到这些变量，因此该程序有许多候选oracle数据1要包含在测试oracle中。
>
> 用于确定程序是否按照给定测试输入的预期执行，它直观地由测试期间要观察的变量及其预期值组成。由于程序通常由各种内部变量和输出变量组成，在软件测试的不同位置可以观察到这些变量，因此该程序有许多候选oracle数据要包含在测试oracle中。
>
> 测试oracle包含的oracle数据越多，测试oracle检测故障的能力就越强。然而使用全部或大部分oracle数据构建测oracle的成本可能非常高，开发人员需要指定这些变量的预期值。因此，出现了oracle数据选择问题，目的是在构建测试oracle时减少oracle数据的数量。
>
> 测试预言验证程序是否按预期运行的机制
>
> 选择正确的oracle数据进行观察是构建测试预言的关键
>
> 高质量的测试预言器对于检测软件故障至关重要。



问题

研究人员提出了各种技术来自动生成测试输入，但测试预言问题仍然被认为是软件测试中最困难的问题之一，换而言之有输入玩没输出。

### oracle数据选择的动态方法

分析测试执行信息

* 变量值
* 交互信息

不足：在程序执行期间收集测试执行信息会产生额外的成本。

MAODS和DODONA

MAODS是oracle数据选择的第一种方法，它针对大量突变体运行测试输入，并根据这些变量区别于原始程序的突变体数量选择变量。后来，DODONA仅针对面向对象的程序，提出通过分析执行跟踪中变量关系图的网络中心度度量来选择oracle数据。



###  SODS 

(Static Oracle Data Selection)

静态预言数据选择

> 通过基于被测程序的定义-使用链来构建概率替换图以识别候选oracle数据之间的替换关系，然后估计每个候选oracle数据的故障观察能力，最后选择具有较强故障观察能力的oracle数据子集。

Basic SODS

* 通过基于被测程序的定义-使用链来构建概率替换图，概率替换图是一个表示候选oracle数据在多大程度上可以替代其他数据的图；
* 通过考虑替换关系的转移程度(以α度量)估计每个候选oracle数据在观察每个语句中的错误方面的能力；
* 根据故障观察能力和选定oracle数据的影响(通过FP测量)确定候选oracle数据的选择顺序。

Extended SODS

* Basic SODS；
* 基于0-1-CF测试调用图分析来裁剪被测程序，通过剪枝概率替换图，以提高oracle数据选择的有效性。(只关注程序中被测试的部分)



### oracle数据

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

用于衡量候选oracle数据的故障观察能力

基于定义使用链，我们可以在候选oracle数据之间构建替换关系。

软件故障通常是由错误的代码引起的，代码中的错误可以通过oracle数据观察到。

注意定义-使用链具有**传递性**



如果选定的oracle变量不稳定（例如，在同一测试输入的不同执行期间可能产生不同值的随机变量），我们将其从选定的oracle数据集中删除。







### oracle数据选择

* 由于使用所有候选oracle数据构建测试oracle的成本很高，因此有必要确定这些候选oracle数据的选择顺序，以便开发人员可以使用少量oracle数据构建高质量的测试oracle；
* 直观地说，在构建测试oracle时，具有较大故障观察能力的候选oracle数据往往会提前选择；
* 但是，仅根据oracle数据的故障检测能力排序来选择oracle数据可能不太有效，因为某些候选oracle数据会观察由相同语句导致的故障。

oracle数据选择的目标应该是**最大化选定oracle数据集的故障观察能力**，而不是最大化每个选定oracle数据单独的故障观察能力。



**概率替换图PSG权重分配**

* 如果inojandoi中的两个语句总是一起执行（例如，图1中的O1andO3），则P（oi？oj）设置为1。
* 所有循环将被视为有一次迭代，$while (i > 0) => if (i > 0)$;
* $o_i$和$o_j定义的语句总是一起执行 => $P(o_i > o_j) = 1$;
* 对if语句应用乘法原理，例如$P(o_6 > o_3) = \overline{b_1} * \overline{b_2} * b_3$， $b_1$、$b_2$和$b_3$分别表示$n < 0$、$n == 0$和$i > 0$的概率



### 估计故障观测能力

对于任何候选oracle  dataoi，我们遍历PSG以查找其故障可能通过observingoi检测到的语句集。我们将该集合表示为asW（oi）。显然，W（oi）包括inoi语句。对于属于toW（oi）的anyoj，它可能会通过observingoi检测由inojvia语句引起的故障。也就是说，OI具有观察inoj语句导致的故障的能力。我们将这种能力表示为OC（i，j）。

这里，α（可以是常数或变量）是一个参数，用于测量替代关系的程度-在具有长度的路径中，两条相邻边之间的映射传递≥2.

$W(o_i)$: 遍历PSG以查找通过$o_i$可以检测到故障的所有oracle数据集合(包括$o_i$本身)(数据？语句？)

$FOC(i, j)$: $o_i$检测$o_j$导致的故障的能力大小



FOC计算

* $FOC(i, i) = 1$
* $o_i <= o_j => FOC(i, j) = P(o_i > o_j)$
* $FOC(path) = α ^ {s - 1} * P(o_i, o_{k1}) * ... * P(o_{ks - 1}, o_{ks})$, $α$用于测量替代关系的程度
* $FOC(i, j) = \sum^l_{t = 1}{path_t}$
* $FOC(i) = \sum^{}_{o_j \in W(o_i)}{FOC(i, j)}$

![](C:\Users\linzs148\Desktop\工具\MYSODS\resources\images\graph.png)



### 基于启发式算法的oracle数据选择

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



### $f_p$的选择

我们使用FP测量选定oracle数据对未选定oracle数据的影响。

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



### 程序剪裁

* 在实践中测试通常是不充分的，因此不必考虑所有候选oracle数据之间的替换关系。因此，我们可以通过只关注正在测试的部分程序，进一步提高静态oracle数据选择的有效性。
* 对于任何具有可分析代码段形式的测试的程序(例如JUnit测试)，我们进一步扩展了静态技术，以基于静态调用图分析定制程序

在实践中，测试通常是不够的，实际上是部分测试程序。因此，不必考虑候选Oracle数据之间的所有替换关系，因为它们中的一些不被现有测试覆盖。此外，考虑到所有这些替换关系，我们的基本静态技术可能会选择无用的oracle数据，从而降低效率。幸运的是，对于一些现代单元测试框架（例如JUnit），每个测试都是一个代码片段，包括方法调用序列。因此，基于测试代码片段的调用图分析，可以静态地识别测试的源代码部分。也就是说，对于任何具有可分析代码段形式的测试的程序（例如JUnit测试），我们进一步扩展了静态技术，以基于静态调用图分析定制程序。

特别是，我们的扩展技术首先通过使用0-1-CF算法提取测试的静态调用图，这已经被证明是有效的和更精确的比其他常见的静态算法，e。G类层次分析[17]或快速类型分析[4]。基于静态调用图，我们的扩展技术通过删除不在静态调用图中的候选oracle数据来构建定义使用链。

### 实验

关注问题

* 不同的配置(即$α$和$f_p$)如何影响SODS的有效性?
* SODS在有效性和效率方面与现有的动态方法相比如何?
* 选定oracle数据的数量(m)如何影响SODS的有效性？



使用的工具

* Java主题的分析工具WALA



自变量

* oracle数据选择方法
  * SODS
  * MAODS
  * DODONA
* SODS配置
  * 用于测量替代关系的程度的$α$(0, 0.25, 0.5, 1)
  * 选定的oracle数据对不同的未选定oracle数据有不同的影响$f_p$  一个微分值（在本表中缩写为）或一个统一值（即1）。
* 选定oracle数据的大小(1~10) 使用10个oracle数据作为默认配置



因变量

* oracle数据集故障检测率(衡量有效性)
* oracle数据集选择的总时间(效率)



实验过程

1. 记录oracle数据选择总时间
2. 在原始程序执行期间进行代码插装记录oracle数据的实际值来获得相应测试输入的预期值，在代码分析过程中检查故障程序上对应oracle数据的实际值是否与相应的预期值相同，以此来判断是否检测到相应的故障
3. 计算每个方法选择的oracle数据集在20个不同突变组（即800个故障）上检测到的故障率并进行比较



为了评估选定oracle数据的故障检测有效性，我们将带有选定oracle数据的每个测试输入应用于故障程序，记录每个oracle数据检测到的故障。特别是，我们首先通过在原始程序执行期间通过代码插装记录其实际值，从而获得相应测试输入的预期值。然后，我们检查故障程序上此oracle数据的实际值是否与相应的预期值相同。否则，此oracle数据将检测到相应的故障。对于每个受试者，根据每个oracle数据检测到的故障，我们计算每个方法选择的oracle数据集在20个不同突变组（即800个故障）上检测到的故障率。



过程简化

* 假设条件语句中的每个分支都具有相同的执行概率
* 不处理变量的别名
* 在构建候选oracle数据集时，类的每个数组变量、对象变量、堆变量或成员变量都被视为单个变量



实验结果

两个参数的最佳配置为α=0和fp=d

FP=d的所有配置通常至少与FP=1的所有配置一样有效，而α=0的所有配置通常比其他配置性能更好。

替代关系转移不会提高oracle数据选择的有效性，但考虑到选定oracle数据对未选定oracle数据的不同影响，可以提高有效性。

通过删除定义来裁剪PSG，使用我们的扩展静态技术没有包含在测试中，它进一步提高了基本静态技术的有效性。

静态方法在效率方面明显优于动态方法。

对于从1到10的各种oracle数据大小，我们的静态方法是稳定的，并且在大多数情况下比动态技术更有效。

当使用更大的oracle数据量时，不同oracle数据选择技术的有效性趋于饱和。此外，使用10个以上的oracle数据无法提供在故障检测方面有很大的改进。因此，考虑到在实践中手动确定每个oracle数据的预期值的成本，我们使用10个oracle数据的默认设置可能具有成本效益。



通过对α和FP影响的研究，当α设置为0，FP设置为差异时，我们的SODS技术更有效。

通过比较动态方法和静态方法的研究，我们的SODS在大多数情况下比动态方法（包括MAODS和DODONA）更有效，也比动态方法，尤其是MAODS更有效。

对于具有可分析测试代码的程序（例如JUnit测试），我们的扩展静态技术甚至比基本静态技术更有效。



未来工作

我们计划通过进一步优化来改进我们的基本技术，例如正交列表表示[35]和Johnson算法[44]，其目标是存储和操作稀疏矩阵。

由于我们的静态方法也可能受到静态分析的固有限制（例如缺少动态类加载），我们计划进一步结合动态和静态方法的优点。



## 论文复现



### 复现重点

- 概率替换图的构建
- 预言数据权重的生成
- 预言数据的选择算法







下载wala JAR包

导包

写exclusions.txt和scope.txt

























https://blog.csdn.net/weixin_44712778/article/details/113105671

https://github.com/SCanDroid/SCanDroid

https://github.com/Athithyaa/ConstantPropagationWala/blob/master/primordial.jar.model





### 实验日志

### 2021/11/25

* 搭建了整个工具复现项目的框架
* 实现了从单个方法中提取赋值语句带来的def-use关系矩阵



### 2021/11/26

* 基于论文实现了所有的def-use对替换的概率都为1的情况

