package com.xuanchen.monitor.server.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.monitor.server.entity.DiskInfoVO;
import com.xuanchen.monitor.server.entity.ServerInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 控制器-->性能监控
 *
 * @author XuanChen
 * @date 2026-02-02
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController {
    @GetMapping("/info")
    public Result getServerInfo() {
        ServerInfoVO serverInfo = getServerDetailInfo();
        return Result.success(serverInfo);
    }

    private ServerInfoVO getServerDetailInfo() {
        ServerInfoVO serverInfo = new ServerInfoVO();
        // 创建系统信息实例
        SystemInfo systemInfo = new SystemInfo();
        // 获取操作系统信息
        OperatingSystem os = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        // 设置CPU信息
        setCpuInfo(serverInfo, hardware);
        // 设置内存信息
        setMemoryInfo(serverInfo, hardware);
        // 设置JVM信息
        setJvmInfo(serverInfo);
        // 设置系统信息
        setSysInfo(serverInfo, os);
        // 设置磁盘信息
        setDiskInfo(serverInfo, hardware);
        return serverInfo;
    }

    // 设置CPU信息
    private void setCpuInfo(ServerInfoVO serverInfo, HardwareAbstractionLayer hardware) {
        CentralProcessor processor = hardware.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(1000);
        long[] ticks = processor.getSystemCpuLoadTicks();

        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks, ticks);

        double currentLoad = processor.getSystemCpuLoad(10);

        serverInfo.setCpuNum(processor.getLogicalProcessorCount());
        serverInfo.setCpuUsed(String.format("%.2f", cpuUsage * 100));
        serverInfo.setCpuSys(String.format("%.2f", currentLoad * 100));
        serverInfo.setCpuFree(String.format("%.2f", (1 - cpuUsage) * 100));
    }

    // 设置内存信息
    private void setMemoryInfo(ServerInfoVO serverInfo, HardwareAbstractionLayer hardware) {
        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long usedMemory = totalMemory - memory.getAvailable();

        serverInfo.setTotalMem(totalMemory / (1024 * 1024 * 1024)); // GB
        serverInfo.setUsedMem(usedMemory / (1024 * 1024 * 1024));
        serverInfo.setFreeMem((totalMemory - usedMemory) / (1024 * 1024 * 1024));
        serverInfo.setMemUsage(String.format("%.2f", (double) usedMemory / totalMemory * 100));
    }

    // 设置JVM信息
    private void setJvmInfo(ServerInfoVO serverInfo) {
        Properties props = System.getProperties();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        serverInfo.setJvmTotal(totalMemory / (1024 * 1024));
        serverInfo.setJvmMax(maxMemory / (1024 * 1024));
        serverInfo.setJvmFree(freeMemory / (1024 * 1024));
        serverInfo.setJvmUsed(usedMemory / (1024 * 1024));
        serverInfo.setJvmUsage(String.format("%.2f", (double) usedMemory / totalMemory * 100));
        serverInfo.setJvmName(props.getProperty("java.vm.name"));
        serverInfo.setJvmVersion(props.getProperty("java.version"));
    }

    // 设置系统信息
    private void setSysInfo(ServerInfoVO serverInfo, OperatingSystem os) {
        serverInfo.setOsName(os.getBitness() + "-bit " + os.toString());
        serverInfo.setComputerName(System.getenv().getOrDefault("COMPUTERNAME", "Unknown"));
        serverInfo.setUserDir(System.getProperty("user.dir"));
    }

    // 设置磁盘信息
    private void setDiskInfo(ServerInfoVO serverInfo, HardwareAbstractionLayer hardware) {
        List<HWDiskStore> diskStores = hardware.getDiskStores();
        List<DiskInfoVO> diskInfos = new ArrayList<>();

        for (HWDiskStore disk : diskStores) {
            long readBytes = disk.getReadBytes();
            long writeBytes = disk.getWriteBytes();
            long readCount = disk.getReads();
            long writeCount = disk.getWrites();

            DiskInfoVO diskInfo = new DiskInfoVO();
            diskInfo.setRead(readBytes);
            diskInfo.setWrite(writeBytes);
            diskInfo.setReadCount(readCount);
            diskInfo.setWriteCount(writeCount);
            diskInfo.setName(disk.getName());

            // 设置磁盘分区信息
            List<HWPartition> partitions = disk.getPartitions();
            for (HWPartition partition : partitions) {
                File f = new File(partition.getMountPoint());
                if (f.exists()) {
                    long total = f.getTotalSpace();
                    long free = f.getFreeSpace();
                    long used = total - free;

                    diskInfo.setTotal(total / (1024 * 1024 * 1024));
                    diskInfo.setFree(free / (1024 * 1024 * 1024));
                    diskInfo.setUsed(used / (1024 * 1024 * 1024));
                    diskInfo.setUsage(String.format("%.2f", (double) used / total * 100));
                }
            }
            diskInfos.add(diskInfo);
        }
        serverInfo.setDiskInfos(diskInfos);
    }
}
