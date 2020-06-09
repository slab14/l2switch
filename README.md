# l2switch
This is a branch of the OpenDayLight - Aluminum version.  This branch removes the dependency on leveldbjni, which allows the controller to run on ARM32 architecture (e.g. - Raspberry Pi 4)

**Purposes:** Branch is intended to be tested with x86 architectuce environments like CloudLab VMs used in our [demo](https://github.com/slab14/IoT_Sec_Gateway).

**How it works:**

 ![image](https://i.ibb.co/44GHvNQ/image.png)
 
 
**1) General setup**
  - Run the following command to copy the Github repository to your __dataplane nodes__: `git clone https://github.com/brytul/l2switch.git`
  - On your dataplane node, `cd` into the repository and run **build.sh** and **startODL.sh**.
    - __Note__: SDN controller assumes a virtual switch is running locally named br0 listening on port 6677.  This build also assumes Docker is running and listening on port 4243. You will need to use [our branch](https://github.com/slab14/ovs/tree/slab) of OVS in order to utilize ovs-docker-remote commands.
