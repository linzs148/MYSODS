# MYSODS

## 相关资源链接

[WALA使用总结](https://github.com/linzs148/SODS/blob/main/resources/WALA%20Guide.md)

[实验报告](https://github.com/linzs148/SODS/blob/main/resources/Experiment%20Report.md)

[工具理解PPT](https://docs.qq.com/slide/DZkJSbVhBeFRvaWVW)

[工具展示视频（bilibili）](https://www.bilibili.com/video/BV1DL4y1p7Xd/)

[工具展示视频（百度云）](https://pan.baidu.com/s/14yCnvqMaPXaKPEK2zrOH3g) 提取码：b8lr



## 项目目录结构

```
MYSODS
│  README.md
├─code
│  │  MYSODS.iml
│  │  pom.xml
│  ├─src
│  │  ├─main
│  │  │  ├─java
│  │  │  │  MYSODS.java // 工具复现代码
│  │  │  └─resources // wala配置文件
│  │  │      exclusions.txt
│  │  │      scope.txt
│  │  │      wala.properties
│  │  └─test
│  │      └─java
│  │          Test.java // 工具测试代码
│  └─target
│      ├─classes
│      │   exclusions.txt
│      │   MYSODS.class
│      │   scope.txt
│      │   wala.properties
│      └─test-classes
│          Test.class
└─resources
    │  Experiment Report.md // 实验报告
    │  SODS.pptx // 工具理解PPT
    │  WALA Guide.md // WALA使用总结
    │  [ASE'16]Supporting_oracle_construction_via_static_analysis.pdf // 工具复现论文
    │  [TSE'14]The_Oracle_Problem_in_Software-A_Survey.pdf
    ├─images // 相关截图
    └─WALA // wala jar包
```

