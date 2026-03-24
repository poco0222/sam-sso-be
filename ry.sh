#!/bin/sh
# 文件用途：管理 yr-admin 打包产物的启动、停止、重启与状态检查
# 作者：yr
# 创建时间：历史脚本，2026-03-10 调整为兼容 Java 17 的启动参数与顺序

APP_NAME=yr-admin.jar

# Java 17 兼容的 JVM 参数，移除 PermSize 与旧 GC 日志参数
JVM_OPTS="-Dname=$APP_NAME -Duser.timezone=Asia/Shanghai -Xms512M -Xmx512M -XX:+HeapDumpOnOutOfMemoryError -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC"
APP_HOME=`pwd`
LOG_PATH=$APP_HOME/logs/$APP_NAME.log

if [ "$1" = "" ]; then
    echo -e "[0;31m 未输入操作名 [0m  [0;34m {start|stop|restart|status} [0m"
    exit 1
fi

if [ "$APP_NAME" = "" ]; then
    echo -e "[0;31m 未输入应用名 [0m"
    exit 1
fi

start() {
    PID=`ps -ef | grep java | grep $APP_NAME | grep -v grep | awk '{print $2}'`

    if [ x"$PID" != x"" ]; then
        echo "$APP_NAME is running..."
    else
        # 先注入 JVM 参数，再指定 -jar，避免参数被误当成应用参数
        nohup java $JVM_OPTS -jar target/$APP_NAME > /dev/null 2>&1 &
        echo "Start $APP_NAME success..."
    fi
}

stop() {
    echo "Stop $APP_NAME"

    PID=""
    query() {
        PID=`ps -ef | grep java | grep $APP_NAME | grep -v grep | awk '{print $2}'`
    }

    query
    if [ x"$PID" != x"" ]; then
        kill -TERM $PID
        echo "$APP_NAME (pid:$PID) exiting..."
        while [ x"$PID" != x"" ]
        do
            sleep 1
            query
        done
        echo "$APP_NAME exited."
    else
        echo "$APP_NAME already stopped."
    fi
}

restart() {
    stop
    sleep 2
    start
}

status() {
    PID=`ps -ef | grep java | grep $APP_NAME | grep -v grep | wc -l`
    if [ $PID != 0 ]; then
        echo "$APP_NAME is running..."
    else
        echo "$APP_NAME is not running..."
    fi
}

case $1 in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo -e "[0;31m 输入参数错误 [0m  [0;34m {start|stop|restart|status} [0m"
        exit 1
        ;;
esac
