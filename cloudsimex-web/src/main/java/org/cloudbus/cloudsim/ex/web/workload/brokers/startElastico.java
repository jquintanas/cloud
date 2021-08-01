package org.cloudbus.cloudsim.ex.web.workload.brokers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.ex.DatacenterBrokerEX;
import org.cloudbus.cloudsim.ex.MonitoringBorkerEX;
import org.cloudbus.cloudsim.ex.disk.DataItem;
import org.cloudbus.cloudsim.ex.disk.HddCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.ex.disk.HddHost;
import org.cloudbus.cloudsim.ex.disk.HddPe;
import org.cloudbus.cloudsim.ex.disk.VmDiskScheduler;
import org.cloudbus.cloudsim.ex.util.CustomLog;
import org.cloudbus.cloudsim.ex.util.TextUtil;
import org.cloudbus.cloudsim.ex.vm.MonitoredVMex;
import org.cloudbus.cloudsim.ex.web.WebCloudlet;
import org.cloudbus.cloudsim.ex.web.workload.StatWorkloadGenerator;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class startElastico {
	private static DataItem data = new DataItem(5);
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		int appID = 32;    
		Properties props = new Properties();
        try (InputStream is = Files.newInputStream(Paths.get("../custom_log.properties"))) {
            props.load(is);
        }
        CustomLog.configLogger(props);
        CustomLog.printLine("Example of Web broker.");
        int numBrokers = 1;         // number of brokers we'll be using
        boolean trace_flag = false; // mean trace events
        
    	// Initialize CloudSim
        CloudSim.init(numBrokers, Calendar.getInstance(), trace_flag);
        DatacenterBrokerEX datacenter0 = createDatacenter("WebDataCenter");
        MonitoringBorkerEX broker = new MonitoringBorkerEX("Broker", -1, 1, 1);
        SimpleAutoScalingPolicy auto = new SimpleAutoScalingPolicy(appID, 0.2, 0.1, 20);
        broker.addAutoScalingPolicy(auto);
        broker.recordUtilisationPeriodically(1);
        List<MonitoredVMex> vmlist = new ArrayList<MonitoredVMex>();
     // VM description
        int mips = 250;
        int ioMips = 200;
        long size = 10000;  // image size (MB)
        int ram = 512;      // vm memory (MB)
        long bw = 1000;
        int pesNumber = 1;  // number of cpus
        String vmm = "Xen"; // VMM name
        for (int i = 0; i < 5; i++) {
        	MonitoredVMex vm = new MonitoredVMex("App-Srv", broker.getId(), mips, pesNumber, ram, bw, size, vmm, new HddCloudletSchedulerTimeShared(), 1);
        	vmlist.add(vm);
		}
        //List<StatWorkloadGenerator> workloads = generateWorkloads();
	    broker.submitVmList(vmlist);
	 // Sixth step: Starts the simulation
	    CloudSim.startSimulation();

	    // Final step: Print results when simulation is over
	    List<Cloudlet> newList = broker.getCloudletReceivedList();

	    CloudSim.stopSimulation();

	    printCloudletList(newList);

	    // Print the debt of each user to each datacenter
	    // datacenter0.printDebts();

	    System.err.println("\nSimulation is finished!");
	}
	
	private static DatacenterBrokerEX createDatacenter(final String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        // our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<>();
        List<HddPe> hddList = new ArrayList<>();

        int mips = 1000;
        int iops = 1000;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store
        // Pe id and
        // MIPS Rating

        hddList.add(new HddPe(new PeProvisionerSimple(iops), data));

        // 4. Create Host with its id and list of PEs and add them to the list
        // of machines
        int ram = 1024; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 10000;

        // This is our machine
        hostList.add(new HddHost(new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, hddList,
                new VmSchedulerTimeShared(peList), new VmDiskScheduler(hddList)));

        // 5. Create a DatacenterCharacteristics object that stores the
        // properties of a data center: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone
        // and its price (G$/Pe time unit).
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
                cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        DatacenterBrokerEX datacenter = null;
        try {
            /*datacenter = new HddDataCenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList,
                    0);*/
        	datacenter = new DatacenterBrokerEX(name, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

	   private static void printCloudletList(final List<Cloudlet> list) {
			int size = list.size();
			Cloudlet cloudlet;

			// Print header line
			CustomLog.printLine(TextUtil.getCaptionLine(WebCloudlet.class));

			// Print details for each cloudlet
			for (int i = 0; i < size; i++) {
			    cloudlet = list.get(i);
			    CustomLog.print(TextUtil.getTxtLine(cloudlet));
			}
		    }

}
