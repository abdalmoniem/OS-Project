/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main_Package;


public class Memory_Location 
{
    public int Usage;
    public int Allocation_Time;
    public int PID;
    public int Data;
    /**
     * 
     * @param Usage allocate the usage no of each page
     * @param Allocation_Time put the allocation time
     * @param PID allocate pid
     * @param Data record data in each page
     */
    public Memory_Location(int Usage,int Allocation_Time,int PID,int Data)
    {
        this.Usage=Usage;
        this.Allocation_Time=Allocation_Time;
        this.PID=PID;
        this.Data=Data;
    }
}
