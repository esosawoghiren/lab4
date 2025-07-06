public class KernelFunctions
{
	//******************************************************************
	//                 Methods for supporting page replacement
	//******************************************************************

	// the following method calls a page replacement method once
	// all allocated frames have been used up
	// DO NOT CHANGE this method
	public static void pageReplacement(int vpage, Process prc, Kernel krn)
	{
	   if(prc.pageTable[vpage].valid) return;   // no need to replace

	   if(!prc.areAllocatedFramesFull()) // room to get frames in allocated list
 	       addPageFrame(vpage, prc, krn);
	   else
	       pageReplAlgorithm(vpage, prc, krn);
	}

	// This method will all a page frame to the list of allocated 
	// frames for the process and load it with the virtual page
	// DO NOT CHANGE this method
	public static void addPageFrame(int vpage, Process prc, Kernel krn)
	{
	     int [] fl;  // new frame list
	     int freeFrame;  // a frame from the free list
	     int i;
	     // Get a free frame and update the allocated frame list
	     freeFrame = krn.getNextFreeFrame();  // gets next free frame
	     if(freeFrame == -1)  // list must be empty - print error message and return
	     {
		System.out.println("Could not get a free frame");
		return;
	     }
	     if(prc.allocatedFrames == null) // No frames allocated yet
	     {
		prc.allocatedFrames = new int[1];
		prc.allocatedFrames[0] = freeFrame;
	     }
	     else // some allocated but can get more
	     {
	        fl = new int[prc.allocatedFrames.length+1];
	        for(i=0 ; i<prc.allocatedFrames.length ; i++) fl[i] = prc.allocatedFrames[i];
	        fl[i] = freeFrame; // adds free frame to the free list
	        prc.allocatedFrames = fl; // keep new list
	     }
	     // update Page Table
	     prc.pageTable[vpage].frameNum = freeFrame;
	     prc.pageTable[vpage].valid = true;
	}

	// Calls to Replacement algorithm
	public static void pageReplAlgorithm(int vpage, Process prc, Kernel krn)
	{
	     boolean doingCount = false;
	     switch(krn.pagingAlgorithm)
	     {
		case FIFO: pageReplAlgorithmFIFO(vpage, prc); break;
		case LRU: pageReplAlgorithmLRU(vpage, prc); break;
		case CLOCK: pageReplAlgorithmCLOCK(vpage, prc); break;
		case COUNT: pageReplAlgorithmCOUNT(vpage, prc); doingCount=true; break;
	     }
	}
	
	//--------------------------------------------------------------
	//  The following methods need modification to implement
	//  the three page replacement algorithms
	//  ------------------------------------------------------------
	// The following method is called each time an access to memory
	// is made (including after a page fault). It will allow you
	// to update the page table entries for supporting various
	// page replacement algorithms.
	public static void doneMemAccess(int vpage, Process prc, double clock)
	{


		// Update used bit for CLOCK algorithm
		prc.pageTable[vpage].used = true;

		// Update timestamp for LRU algorithm
		prc.pageTable[vpage].tmStamp = clock;

		// Increment count for COUNT algorithm
		prc.pageTable[vpage].count++;

	}

	// FIFO page Replacement algorithm
	public static void pageReplAlgorithmFIFO(int vpage, Process prc)
	{
	   int frame;
	   int vPageReplaced;

	   for(int i = 0; i < prc.pageTable.length; i++){
			if (prc.fifoSet.size() < prc.allocatedFrames.length){
				//add page into the set and queue if it's not present
				if (!prc.fifoSet.contains(vpage)) {
					frame = prc.allocatedFrames[prc.fifoSet.size()]; // assign next free frame
					// load the page
					prc.pageTable[vpage].frameNum = frame;
					prc.pageTable[vpage].valid = true;
					// add to both set and queue
					prc.fifoSet.add(vpage);
					prc.fifoQueue.add(vpage);
				}
				//we perform the fifo if the set is full
			}else{
				if (!prc.fifoSet.contains(vpage)){
				//check to see if the page isn't already present
				if (!prc.fifoSet.contains(vpage)) {

					//remove head object in queue and set
					vPageReplaced = prc.fifoQueue.poll();
					prc.fifoSet.remove(vPageReplaced);
					//get frame of replaced page and set it to unallocated
					frame = prc.pageTable[vPageReplaced].frameNum;
					prc.pageTable[vPageReplaced].valid = false;
					// load new page into replaced page spot
					prc.pageTable[vpage].frameNum = frame;
					prc.pageTable[vpage].valid = true;
					// add new page to set and queue
					prc.fifoSet.add(vpage);
					prc.fifoQueue.add(vpage);
				}
			}
		}

	}
	}
	

	// CLOCK page Replacement algorithm
	public static void pageReplAlgorithmCLOCK(int vpage, Process prc)
	{
		int frame;
		int vPageReplaced;

		while(true) {
			// Get next frame in clock order
			frame = prc.allocatedFrames[prc.framePtr];
			vPageReplaced = findvPage(prc.pageTable, frame);

			// If used bit is set, give it a second chance
			if(prc.pageTable[vPageReplaced].used) {
				prc.pageTable[vPageReplaced].used = false;
				prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length;
			}
			else {
				// Replace this page
				prc.pageTable[vPageReplaced].valid = false;
				prc.pageTable[vpage].frameNum = frame;
				prc.pageTable[vpage].valid = true;
				prc.pageTable[vpage].used = true;
				prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length;
				break;
			}
		}
	}

	// LRU page Replacement algorithm
	public static void pageReplAlgorithmLRU(int vpage, Process prc)
	{
		int lruPage = -1;
		double minTime = Double.MAX_VALUE;

		// Find the page with the smallest timestamp (least recently used)
		for(int i = 0; i < prc.pageTable.length; i++) {
			if(prc.pageTable[i].valid && prc.pageTable[i].tmStamp < minTime) {
				minTime = prc.pageTable[i].tmStamp;
				lruPage = i;
			}
		}

		if(lruPage != -1) {
			int frame = prc.pageTable[lruPage].frameNum;
			prc.pageTable[lruPage].valid = false;
			prc.pageTable[vpage].frameNum = frame;
			prc.pageTable[vpage].valid = true;
			prc.pageTable[vpage].tmStamp = System.currentTimeMillis();
		}
	}



	// COUNT page Replacement algorithm
	public static void pageReplAlgorithmCOUNT(int vpage, Process prc)
	{
		int frame;
		int vPageReplaced;
		int lowestCountPage = -1;
		long lowestCount = Long.MAX_VALUE;
		int startPtr = prc.framePtr;

		// First pass: look for a page with count = 0
		while(true) {
			frame = prc.allocatedFrames[prc.framePtr];
			vPageReplaced = findvPage(prc.pageTable, frame);

			// Track page with lowest count in case we don't find count=0
			if(prc.pageTable[vPageReplaced].count < lowestCount) {
				lowestCount = prc.pageTable[vPageReplaced].count;
				lowestCountPage = vPageReplaced;
			}

			// If count is 0, replace this page
			if(prc.pageTable[vPageReplaced].count == 0) {
				prc.pageTable[vPageReplaced].valid = false;
				prc.pageTable[vpage].frameNum = frame;
				prc.pageTable[vpage].valid = true;
				prc.pageTable[vpage].used = true;
				prc.pageTable[vpage].count = 3;
				prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length;
				return;
			}

			// divide the count by 3 to age the counts
			prc.pageTable[vPageReplaced].count /= 3;

			prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length;

			// If we've gone all the way around, break
			if(prc.framePtr == startPtr) break;
		}

		// replace the one with lowest count
		if(lowestCountPage != -1) {
			frame = prc.pageTable[lowestCountPage].frameNum;
			prc.pageTable[lowestCountPage].valid = false;
			prc.pageTable[vpage].frameNum = frame;
			prc.pageTable[vpage].valid = true;
			prc.pageTable[vpage].used = true;
			prc.pageTable[vpage].count = 3;
			prc.framePtr = (prc.framePtr + 1) % prc.allocatedFrames.length;
		}

	}

	// finds the virtual page loaded in the specified frame
	public static int findvPage(PgTblEntry [] ptbl, int fr)
	{
	   int i;
	   for(i=0 ; i<ptbl.length ; i++)
	   {
	       if(ptbl[i].valid)
	       {
	          if(ptbl[i].frameNum == fr)
	          {
		      return(i);
	          }
	       }
	   }
	   System.out.println("Could not find frame number in Page Table "+fr);
	   return(-1);
	}

	// *******************************************
	// The following method is provided for debugging purposes
	// Call it to display the various data structures defined
	// for the process so that you may examine the effect
	// of your page replacement algorithm on the state of the 
	// process.
        // Method for displaying the state of a process
	// *******************************************
	public static void logProcessState(Process prc)
	{
	   int i;

	   System.out.println("--------------Process "+prc.pid+"----------------");
	   System.out.println("Virtual pages: Total: "+prc.numPages+
			      " Code pages: "+prc.numCodePages+
			      " Data pages: "+prc.numDataPages+
			      " Stack pages: "+prc.numStackPages+
			      " Heap pages: "+prc.numHeapPages);
	   System.out.println("Simulation data: numAccesses left in cycle: "+prc.numMemAccess+
			      " Num to next change in working set: "+prc.numMA2ChangeWS);
           System.out.println("Working set is :");
           for(i=0 ; i<prc.workingSet.length; i++)
           {
              System.out.print(" "+prc.workingSet[i]);
           }
           System.out.println();
	   // page Table
	   System.out.println("Page Table");
	   if(prc.pageTable != null)
	   {
	      for(i=0 ; i<prc.pageTable.length ; i++)
	      {
		 if(prc.pageTable[i].valid) // its valid printout the data
		 {
	           System.out.println("   Page "+i+"(valid): "+
				      " Frame "+prc.pageTable[i].frameNum+
				      " Used "+prc.pageTable[i].used+
				      " count "+prc.pageTable[i].count+
				      " Time Stamp "+prc.pageTable[i].tmStamp);
		 }
		 else System.out.println("   Page "+i+" is invalid (i.e not loaded)");
	      }
	   }
	   // allocated frames
	   System.out.println("Allocated frames (max is "+prc.numAllocatedFrames+")"+
			      " (frame pointer is "+prc.framePtr+")");
	   if(prc.allocatedFrames != null)
	   {
	      for(i=0 ; i<prc.allocatedFrames.length ; i++)
 	           System.out.print(" "+prc.allocatedFrames[i]);
	   }
	   System.out.println();
           System.out.println("---------------------------------------------");
	}
}


