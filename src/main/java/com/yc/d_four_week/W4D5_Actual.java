public class W4D5_Actual {

    public static void main(String[] args) {
        // 配置一套常见 JVM 启动参数（含 G1、GC 日志输出），运行程序观察 GC 日志

        /**
         *
         * 堆内存配置（初始堆 = 最大堆，生产环境固定，避免抖动）
         * -Xms2g           # 初始堆大小 2G
         * -Xmx2g           # 最大堆大小 2G
         *
         * G1 垃圾收集器配置
         * -XX:+UseG1GC                  # 启用 G1 垃圾收集器（JDK8+ 默认推荐）
         * -XX:MaxGCPauseMillis=200      # G1 目标最大停顿时间 200ms（核心调优参数）
         *
         * GC 日志输出配置（JDK8 版本）
         * -XX:+PrintGCDetails           # 打印详细 GC 日志
         * -XX:+PrintGCDateStamps        # 打印 GC 发生的日期时间
         * -XX:+PrintGCTimeStamps        # 打印 JVM 启动到 GC 的时间戳
         * -XX:+PrintHeapAtGC            # 每次 GC 前后打印堆内存使用情况
         * -Xloggc:gc.log                # GC 日志输出到当前目录 gc.log 文件
         * -XX:+UseGCLogFileRotation     # 开启 GC 日志滚动切割
         * -XX:NumberOfGCLogFiles=5      # 最多保留 5 个 GC 日志文件
         * -XX:GCLogFileSize=50M         # 每个日志文件最大 50M
         *
         */


        // 用 jstat -gcutil 实时监控 GC 情况

        /**
         *
         * ① 先查进程 PID
         * jps -l   # 列出所有 Java 进程，显示 PID + 主类全类名
         *
         * ② 实时监控 GC（带注释）
         * jstat -gcutil 你的PID 1000 100
         * # 说明：
         * # -gcutil   以百分比形式展示 GC 使用率（最常用）
         * # 1000      每 1000ms（1秒）输出一次
         * # 100       连续输出 100 次（去掉就是无限输出）
         *
         * jstat -gcutil 输出字段
         * S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT
         * 0.00   0.00  45.30  18.22  94.88  91.75     25    0.326     0    0.000    0.326
         */

        // # 字段中文注释
        //  S0:    Survivor0 区使用百分比
        //  S1:    Survivor1 区使用百分比
        //  E:     Eden 伊甸园区使用百分比
        //  O:     Old 老年代使用百分比
        //  M:     Metaspace 元空间使用百分比
        //  CCS:   类指针压缩空间使用百分比
        //  YGC:   年轻代 GC 总次数
        //  YGCT:  年轻代 GC 总耗时（秒）
        //  FGC:   Full GC 总次数（生产环境 FGC 必须为 0）
        //  FGCT:  Full GC 总耗时
        //  GCT:   所有 GC 总耗时
    }
}
