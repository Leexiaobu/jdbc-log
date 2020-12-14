package com.leexiaobu.model;

/**
 * @author Leexiaobu
 * @date 2020-09-27 11:03:12
 */
public class Statistics {
    private long beginTime;
    private long useTime;
    private String modelType;
    private String hostIp;
    private String hostName;
    private String traceId;

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        // 拦截
        this.beginTime = beginTime;
    }

    public long getUseTime() {
        return useTime;
    }

    public void setUseTime(long useTime) {
        this.useTime = useTime;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "beginTime=" + beginTime +
                ", useTime=" + useTime +
                ", modelType='" + modelType + '\'' +
                ", hostIp='" + hostIp + '\'' +
                ", hostName='" + hostName + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
