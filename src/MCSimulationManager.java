/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cumimpactsa;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import cumimpactsa.Helpers.*;

/**
 *
 * @author ast
 */
public class MCSimulationManager 
{

    //simulation options
    
    public int simulationRuns = 100;
    public String outputFolder="";
    public String prefix="";
    
    public boolean missingStressorData=true; public double missingStressorDataMin=0; public double missingStressorDataMax=0.333;
    public boolean sensitivityScoreErrors=true; public double sensitivityScoreErrorsMin=0; public double sensitivityScoreErrorsMax=0.333;
    
    public boolean pointStressLinearDecay=true; public double pointDataLinearDecayMin=0; public double pointDataLinearDecayMax=20;
    
    public boolean ecologicalThresholds=true; public double ecologicalThresholdMin=0; public double ecologicalThresholdMax=1;
    
    public boolean reducedAnalysisRes=true; public double reducedAnalysisResMin=1; public double reducedAnalysisResMax=4;
    public boolean improvedStressorRes=true;
    
    public boolean impactsAsSum=true; public boolean impactsAsMean=true;
    
    public boolean transformationNone=true; public boolean transformationLog=true; 
    public boolean transformationCut5=true; public boolean transformationPercentile=true;
    
    public boolean multipleEffectsAdditive=true; public boolean multipleEffectsDominant=true; 
    public boolean multipleEffectsDiminishing=true;
    
    boolean createSpatialOutputs = true;
    boolean addRunsToResults = true;
    
    //multi-threading options
    public boolean processing=false;
    public int percentCompleted=0;
    
    private ArrayList<CellRankInfo> cellInfos;
    
    
    
    public ArrayList<StressorRankInfo> stressorInfos;
    public ArrayList<EcocompRankInfo> ecocompInfos;
    public ArrayList<RegionRankInfo> regionInfos;
    
    public MCSimulationManager clone()
    {
        MCSimulationManager copy = new MCSimulationManager();
        copy.addRunsToResults=addRunsToResults;
        copy.cellInfos=cellInfos;
        copy.createSpatialOutputs=createSpatialOutputs;
        copy.ecocompInfos=ecocompInfos;
        copy.ecologicalThresholdMax=ecologicalThresholdMax;
        copy.ecologicalThresholdMin=ecologicalThresholdMin;
        copy.ecologicalThresholds=ecologicalThresholds;
        copy.impactsAsMean=impactsAsMean;
        copy.impactsAsSum=impactsAsSum;
        copy.improvedStressorRes=improvedStressorRes;
        copy.missingStressorData=missingStressorData;
        copy.missingStressorDataMax=missingStressorDataMax;
        copy.missingStressorDataMin=missingStressorDataMin;
        copy.multipleEffectsAdditive=multipleEffectsAdditive;
        copy.multipleEffectsDiminishing=multipleEffectsDiminishing;
        copy.multipleEffectsDominant=multipleEffectsDominant;
        copy.outputFolder=outputFolder;
        copy.percentCompleted=percentCompleted;
        copy.pointDataLinearDecayMax=pointDataLinearDecayMax;
        copy.pointDataLinearDecayMin=pointDataLinearDecayMin;
        copy.pointStressLinearDecay=pointStressLinearDecay;
        copy.reducedAnalysisRes=reducedAnalysisRes;
        copy.reducedAnalysisResMax=reducedAnalysisResMax;
        copy.reducedAnalysisResMin=reducedAnalysisResMin;
        copy.sensitivityScoreErrors=sensitivityScoreErrors;
        copy.sensitivityScoreErrorsMax=sensitivityScoreErrorsMax;
        copy.sensitivityScoreErrorsMin=sensitivityScoreErrorsMin;
        copy.simulationRuns=simulationRuns;
        copy.transformationCut5=transformationCut5;
        copy.transformationLog=transformationLog;
        copy.transformationNone=transformationNone;
        copy.transformationPercentile=transformationPercentile;
        return copy;
    }
    
    //creates a deep copy of the list of spatial data layers, so that each simulation run can be using its own copy
    public ArrayList<SpatialDataLayer> makeLayerListClone(ArrayList<SpatialDataLayer> list)
    {
        ArrayList<SpatialDataLayer> cloneList = new ArrayList<SpatialDataLayer>();
        for(int i=0; i<list.size();i++)
        {
            cloneList.add(list.get(i).clone());
        }
        return cloneList;
    }
    
    public void initializeInfoObjects() 
    {
        SensitivityScoreSet scores = MappingProject.sensitivityScores;
        stressorInfos = new ArrayList<>();
        for(int i =0; i<MappingProject.stressors.size();i++)
        {
           StressorRankInfo newInfo = new StressorRankInfo();
           newInfo.name = MappingProject.stressors.get(i).getName();
           for(int s=0; s<scores.getAllScores().size();s++)
           {
               if(scores.getAllScores().get(s).getStressor().getName().equals(newInfo.name))
               {
                   if(scores.getAllScores().get(s).isActive())
                   {
                       newInfo.active=true;
                       break;
                   }
                   else
                   {
                       newInfo.active=false;
                       break;
                   }
               }
           }
           //System.out.println("((((" + newInfo.active);
           stressorInfos.add(newInfo);
        }
        ecocompInfos=new ArrayList<>();
        for(int i =0; i<MappingProject.ecocomps.size();i++)
        {
           EcocompRankInfo newInfo = new EcocompRankInfo();
           newInfo.name = MappingProject.ecocomps.get(i).getName();
           newInfo.cellSum = MappingProject.ecocomps.get(i).getGrid().getCellSum();
           ecocompInfos.add(newInfo);
        }
        //System.out.println("Setting up analysis regions;");
        ArrayList<Double> regionCodes = MappingProject.regions.grid.getUniqueDataValues();
   
        regionInfos = new ArrayList<RegionRankInfo>();
        double[][] regionData = MappingProject.regions.grid.getData();
        
        for(int r=0; r<regionCodes.size();r++)
        {
            RegionRankInfo newInfo = new RegionRankInfo();
            double code = regionCodes.get(r);
            newInfo.regionCode=code;
            //set region area
            for(int x=0; x<regionData.length; x++)
            {
                for(int y=0; y<regionData[0].length; y++)
                {
                    if(regionData[x][y] == code) {newInfo.nrOfCells++;}
                }
            }
            regionInfos.add(newInfo);
        } 
    }
    
    public ArrayList<SpatialDataLayer> makeLayerListCloneBySpatialDataType(ArrayList<SpatialDataLayer> list, String spatialDataType)
    {
        ArrayList<SpatialDataLayer> cloneList = new ArrayList<SpatialDataLayer>();
        for(int i=0; i<list.size();i++)
        {
            if(list.get(i).getSpatialDataType().equals(spatialDataType))
            {
                cloneList.add(list.get(i).clone());
            }
            else
            {
                cloneList.add(list.get(i));
            }
        }
        return cloneList;
    }
    
    protected void eraseProcessingChains(ArrayList<SpatialDataLayer> list)
    {
        for(int i=0;i<list.size();i++)
        {
            list.get(i).getProcessingChain().clear();
        }
    }
    
    protected void addToProcessingChains(ArrayList<SpatialDataLayer> list,PreProcessor processor)
    {
        for(int i=0;i<list.size();i++)
        {
            list.get(i).getProcessingChain().add(processor);
        }
    }
    

    
 
    

    public void runMCSimulation()	
    {

        initializeInfoObjects();
        initializeRegions();

        if(createSpatialOutputs)
        {
            cellInfos = new ArrayList<CellRankInfo>();
            double[][] data = MappingProject.grid.getEmptyGrid();
            for(int x=0; x<data.length;x++)
            {
                for(int y=0; y<data[0].length;y++)
                {
                    if(data[x][y]!=GlobalResources.NODATAVALUE)
                    {
                        CellRankInfo newInfo = new CellRankInfo();
                        newInfo.x=x;
                        newInfo.y=y; 
                        cellInfos.add(newInfo);
                    }
                }
            }
        }
        else
        {
            cellInfos=null;
        }
             
        try
        {
            //run simulation the specified number of times
            for(int run=0;run<simulationRuns;run++)
            {
                System.out.println(prefix+": Simulation run: " + run);
                //make deep copies of model inputs   
                ArrayList<SpatialDataLayer> stressors = makeLayerListClone(MappingProject.stressors);
                eraseProcessingChains(stressors);
                ArrayList<SpatialDataLayer> ecocomps = makeLayerListClone(MappingProject.ecocomps);
                SensitivityScoreSet scores = MappingProject.sensitivityScores.clone(stressors, ecocomps);
  
                setPointStressLinearDecay(stressors);
                setReducedAnalysisRes(stressors, ecocomps);
                addTransformations(stressors);
                addToProcessingChains(stressors,new Rescaler());

                int activeStressors=disableStressorData(stressors, scores);

                changeSensitivityScores(scores);

                setResponseFunctions(scores);
                
                //run simulation
                Simulator simulator = new Simulator(stressors,ecocomps, MappingProject.regions, scores,getMEM(), getImpactModel(),stressorInfos,ecocompInfos,regionInfos);
    
                //simulator.getResult().name="Simulation run "+run;
                //if(addRunsToResults) {MappingProject.results.add(simulator.getResult());} 
           
                updateStressorResults(stressorInfos);
                updateEcocompResults(ecocompInfos);
                updateRegionResults(regionInfos);
                if(createSpatialOutputs) {updateCellResults(simulator.getResult());}

            }

        } 
        catch (Exception e) {JOptionPane.showMessageDialog(null, e + e.getStackTrace()[0].toString());}
    }

    public void writeResults()
    {
            writeRegionResults(regionInfos);
            writeStressorResults(stressorInfos);
            writeEcocompResults(ecocompInfos);
            if(createSpatialOutputs) {writeCellResults(cellInfos);}
    }
    
    //creates new sensitivty scores from U(a,b) where a and b are the highest and lowest "real" scores in the project;
    //then changes the project's scores to a weighted average (weights randomly taken from interval given by simulation parameters).
    protected void changeSensitivityScores(SensitivityScoreSet scores)
    {
        //System.out.println("    ---- "+scores.getInfo(0).getSensitivityScore());
        double[] scoreErrors = new double[scores.size()];
        for(int i=0; i<scoreErrors.length;i++) {scoreErrors[i] = 0.5*(scores.getMax()-scores.getMin())-(Math.random()*(scores.getMax()-scores.getMin()));} //random errors, can be +/- 50% of score range between original max and min scores
        double parameter = Math.random();
        
        //random seeds for area refiner
        for(int i=0; i<scores.size();i++)
        {
            scores.getAllScores().get(i).changeSensitivtyScore(scores.getInfo(i).getSensitivityScore()+parameter*scoreErrors[i]);
            if(scores.getAllScores().get(i).getSensitivityScore()<scores.getMin()) {scores.getAllScores().get(i).changeSensitivtyScore(scores.getMin());}
            if(scores.getAllScores().get(i).getSensitivityScore()>scores.getMax()) {scores.getAllScores().get(i).changeSensitivtyScore(scores.getMax());}
        }
       
        System.out.println("    "+prefix+"Changed sensitivity scores with errors up to: " + (parameter*0.5*scores.getMax()-scores.getMin()));
    }
    
    //returns a random set of parameter values
    public double[] getRandomVector()
    {
        double[] x = new double[9];
        x[0] = Math.round(3*Math.random())/3.0;
        x[1] = Math.round(3*Math.random())/3.0;
        x[2] = Math.round((3*Math.random())/3.0);
        x[3] = Math.round((3*Math.random())/3.0);
        x[4] = Math.round(Math.random());
        x[5] = 0;
        x[6] = Math.round(Math.random());
        x[7] = Math.round(2*Math.random());
        x[8] = Math.round(2*Math.random());
        
        return x;
    }
    
    
    protected int disableStressorData(ArrayList<SpatialDataLayer> stressors, SensitivityScoreSet scores)
    {
        //set all stressor infos to active
        for(int i=0; i<stressorInfos.size();i++) {stressorInfos.get(i).active=true;}
        
        //randomly select number of stressors to be removed 0.1 0.3 -->
        int removeNr = (int) Math.round((missingStressorDataMin+Math.random()*(missingStressorDataMax-missingStressorDataMin))*stressors.size());
        System.out.println("    "+prefix+": Removing "+removeNr +" stressor layers out of " +stressors.size());
        for(int r=0;r<removeNr;r++)
        {
            //randomly select an item to remove; but set to inactive, rather than removing the stressor!
            int rIndex = (int) Math.floor(Math.random()*stressors.size());
            String removeName = stressors.get(rIndex).getName();
            StressorRankInfo info = getStressorInfoByName(removeName);
            info.active=false;
            boolean isFlagged=false;  //check if already removed (no replacement)
            for(int s=0;s<scores.getAllScores().size();s++)
            {
                if(scores.getAllScores().get(s).getStressor().getName().equals(removeName))
                {
                    if(!scores.getAllScores().get(s).isActive()) {isFlagged=true;}
                    scores.getAllScores().get(s).setActive(false);
                    //isFlagged=true;
                }
            }
            if(isFlagged) r=r-1;
        }

        return stressors.size()-removeNr;
        
    }

    protected void setPointStressLinearDecay(ArrayList<SpatialDataLayer> stressors) 
    {
        //create random distance
        double rDistance = pointDataLinearDecayMin+Math.random()*(pointDataLinearDecayMax-pointDataLinearDecayMin);
        
        int pointDataCount=0;
        for(int i=0; i<stressors.size();i++)
        {
            if(stressors.get(i).getSpatialDataType().equals(GlobalResources.SPATIALDATATYPE_POINT))
            {
                pointDataCount++;
                IdwSpreader spreader = new IdwSpreader();
                spreader.setParamValue("distance", rDistance);
                stressors.get(i).getProcessingChain().add(spreader);
            }
        }
        System.out.println("    "+prefix+": Point stressor decay distance: "+rDistance+"; for all "+pointDataCount+" point data sets.");
    }

    private int getImpactModel() 
    {
        int model=-1;
        String name="";
        if(impactsAsSum && !impactsAsMean) {model = Simulator.IMPACTS_SUM;}
        else if(!impactsAsSum && impactsAsMean) {model = Simulator.IMPACTS_AVG;}
        else
        {
            double r=Math.random();
            if(r<0.5) {model = Simulator.IMPACTS_SUM;name="sum";}
            else {model = Simulator.IMPACTS_AVG;name="mean";}
        }
        System.out.println("    "+prefix+": Impact model: "+name);
        return model;
    }

    private void setResponseFunctions(SensitivityScoreSet scores) 
    {
        int impactsToChange = (int) Math.round((ecologicalThresholdMin+Math.random()*(ecologicalThresholdMax-ecologicalThresholdMin))*scores.size());
        System.out.println("    "+prefix+": Changing " + impactsToChange + " out of " + scores.size() + " response functions to thresholds.");
        Collections.shuffle(scores.getAllScores()); //shuffling creates a random permutation. Thresholds are assigned to the first impactsToChange entries after shuffling.
        for(int i=0;i<impactsToChange;i++)
        {
            double x0 = Math.random()*0.4+0.3; //location of logistic funmction center
            ThresholdResponse r = new ThresholdResponse();
            r.setX0(x0);
            scores.getAllScores().get(i).setResponseFunction(r);
        }
    }
    
    private void addTransformations(ArrayList<SpatialDataLayer> stressors)
    {
        ArrayList<PreProcessor> options = new ArrayList<PreProcessor>();
        if(transformationNone) {options.add(new IdentityProcessor());}
        if(transformationCut5) {options.add(new PercentCutter());}
        if(transformationLog) {options.add(new LogTransformer());}
        if(transformationPercentile) {options.add(new PercentileTransformer());}
        
        int selected = (int) Math.floor(Math.random()*options.size());
        System.out.println("    "+prefix+": Transformation: " + options.get(selected).getName());
        for(int i=0; i<stressors.size();i++)
        {
            stressors.get(i).getProcessingChain().add(options.get(selected));
        }
    }

    private int getMEM() 
    {
        ArrayList<Integer> options=new ArrayList<Integer>();
        if(multipleEffectsAdditive) {options.add(Simulator.MEM_ADDITIVE);}
        if(multipleEffectsDominant) {options.add(Simulator.MEM_DOMINANT);}
        if(multipleEffectsDiminishing) {options.add(Simulator.MEM_DIMINISHING);}
        
        int selection = (int) Math.floor(Math.random()*options.size());
        
        if(options.get(selection)==Simulator.MEM_ADDITIVE) {System.out.println("    "+prefix+": MEM: Additive");}
        else if(options.get(selection)==Simulator.MEM_DOMINANT) {System.out.println("    "+prefix+": MEM: Dominant");}
        else if(options.get(selection)==Simulator.MEM_DIMINISHING) {System.out.println("    "+prefix+"MEM: Diminishing");}
        return options.get(selection);
        
    }

    protected void setImprovedStressorResolution(ArrayList<SpatialDataLayer> stressors) 
    {
        //improving the resolution is a pre-processing step
        int polygonDataCount=0;
        for(int i=0; i<stressors.size();i++)
        {
            if(stressors.get(i).getSpatialDataType().equals(GlobalResources.SPATIALDATATYPE_POLYGON))
            {
                polygonDataCount++;
                AreaRefiner refiner = new AreaRefiner();
                stressors.get(i).getProcessingChain().add(refiner);
            }
        }
        System.out.println("   Improved resolution for all " + polygonDataCount +" rasterized polygon layers.");
    }

    private void setReducedAnalysisRes(ArrayList<SpatialDataLayer> stressors, ArrayList<SpatialDataLayer> ecocomps) 
    {
        int factor = (int) Math.round(reducedAnalysisResMin + Math.random() * (reducedAnalysisResMax-reducedAnalysisResMin));
        System.out.println("    "+prefix+": Reducing analysis resolution by factor " + factor);
        if(factor>1) 
        {
            ResolutionReducer reducer = new ResolutionReducer();
            reducer.setParamValue("factor", factor);
            for(int i=0; i<stressors.size();i++)
            {
                stressors.get(i).getProcessingChain().add(reducer);
            }
            for(int i=0; i<ecocomps.size();i++)
            {
                ecocomps.get(i).getProcessingChain().add(reducer);
            }
        }
    }


    private void writeRegionResults(ArrayList<RegionRankInfo> regionInfos) 
    {
        CsvTable table = new CsvTable();
        table.addColumn("Region");
        table.addColumn("HighestRank");
        table.addColumn("LowestRank");
        table.addColumn("RankRange");
        table.addColumn("InTop25p");
        table.addColumn("InBottom25p");
        
        for(int s=0; s<regionInfos.size(); s++)
        {
            RegionRankInfo info = regionInfos.get(s);
            ArrayList<String> row = new ArrayList<String>();
            row.add(info.regionCode+"");
            row.add(info.maxRank+"");
            row.add(info.minRank+"");
            row.add(info.maxRank-info.minRank+"");
            row.add(info.inTop25p/(0.01*simulationRuns)+"");
            row.add(info.inBottom25p/(0.01*simulationRuns)+"");
            table.addRow(row);
        }
        
        table.writeToFile(new File(outputFolder,"regionranks.csv").getAbsolutePath());
         
    }

    private void updateStressorResults(ArrayList<StressorRankInfo> stressorInfos) 
    {
        //count active stressors
        int activeStressors=0;
        for(int i=0; i<stressorInfos.size();i++) {if(stressorInfos.get(i).active) {activeStressors++;}}
        //sort the stressors
       Collections.sort(stressorInfos, new StressorComparator());
       
       int p25 = (int) Math.round(0.25 * activeStressors);
       int p75 = (int) Math.round(0.75 * activeStressors);
       
       int rank=0;
       for(int i=0; i<stressorInfos.size();i++)
       {
           StressorRankInfo info = stressorInfos.get(i);
           if(info.active)
           {
                rank++;
                float nrank=(float) (1.0-(rank-1.0)/activeStressors);
                if(nrank < info.minRank) 
                    {
                        info.minRank=nrank;
                    }
                if(nrank > info.maxRank) {info.maxRank=nrank;}
                if(rank <= p25) {info.inMostImportant25p++;}
                if(rank >= p75) {info.inLeastImportant25p++;}
                info.included++;
           }
       }  
    }

    private void writeStressorResults(ArrayList<StressorRankInfo> stressorInfos) 
    {
        CsvTable table = new CsvTable();
        table.addColumn("Stressor");
        table.addColumn("Times included");
        table.addColumn("HighestRank");
        table.addColumn("LowestRank");
        table.addColumn("RankRange");
        table.addColumn("InTop25p");
        table.addColumn("InBottom25p");
        
        for(int s=0; s<stressorInfos.size(); s++)
        {
            StressorRankInfo info = stressorInfos.get(s);
            ArrayList<String> row = new ArrayList<String>();
            row.add(info.name);
            row.add(info.included+"");
            row.add(info.maxRank+"");
            row.add(info.minRank+"");
            row.add(info.maxRank-info.minRank+"");
            row.add(info.inMostImportant25p/(0.01*info.included)+"");
            row.add(info.inLeastImportant25p/(0.01*info.included)+"");
            table.addRow(row);
        }
        
        table.writeToFile(new File(outputFolder,"stressorranks.csv").getAbsolutePath());
    }

    private ArrayList<RegionRankInfo> initializeRegions() 
    {
        System.out.println(prefix+ ": Setting up analysis regions;");
        ArrayList<Double> regionCodes = MappingProject.regions.grid.getUniqueDataValues();
        ArrayList<RegionRankInfo> regionInfos = new ArrayList<RegionRankInfo>();
        double[][] regionData = MappingProject.regions.grid.getData();
        
        for(int r=0; r<regionCodes.size();r++)
        {
            RegionRankInfo newInfo = new RegionRankInfo();
            double code = regionCodes.get(r);
            newInfo.regionCode=code;
            //set region area
            for(int x=0; x<regionData.length; x++)
            {
                for(int y=0; y<regionData[0].length; y++)
                {
                    if(regionData[x][y] == code) {newInfo.nrOfCells++;}
                }
            }
            regionInfos.add(newInfo);
        } 
        return regionInfos;
    }


    private void updateRegionResults(ArrayList<RegionRankInfo> regionInfos) 
    {
         //calculate mean impact and sort the regions
        for(int i=0; i<regionInfos.size();i++)
        {
            regionInfos.get(i).currentMeanImpact = regionInfos.get(i).currentTotalImpact / regionInfos.get(i).nrOfCells;
        }
       Collections.sort(regionInfos, new RegionComparator());
       
       //save ranks
       int p25 = (int) Math.round(regionInfos.size() * 0.25);
       for(int i=0; i<regionInfos.size();i++)
       {
           RegionRankInfo info = regionInfos.get(i);
           int rank = i+1;
           if(rank < info.minRank) {info.minRank=rank;}
           if(rank > info.maxRank) {info.maxRank=rank;}
           if(rank <= p25) {info.inTop25p++;}
           if(rank >= regionInfos.size()-(p25-1)) {info.inBottom25p++;}
       }  
    }

    private void updateEcocompResults(ArrayList<EcocompRankInfo> ecocompInfos) 
    {
        Collections.sort(ecocompInfos, new EcocompComparator());
        int p25 = (int) Math.round(ecocompInfos.size() * 0.25);
        for(int i=0; i<ecocompInfos.size();i++)
        {
           EcocompRankInfo info = ecocompInfos.get(i);
           int rank = i+1;
           if(rank < info.minRank) {info.minRank=rank;}
           if(rank > info.maxRank) {info.maxRank=rank;}
           if(rank <= p25) {info.inMostImportant25p++;}
           if(rank >= ecocompInfos.size()-(p25-1)) {info.inLeastImportant25p++;}
       }  
    }

    private void writeEcocompResults(ArrayList<EcocompRankInfo> ecocompInfos) 
    {
        CsvTable table = new CsvTable();
        table.addColumn("Ecocomp");
        table.addColumn("Cellsum");
        table.addColumn("HighestRank");
        table.addColumn("LowestRank");
        table.addColumn("RankRange");
        table.addColumn("InTop25p");
        table.addColumn("InBottom25p");
        
        for(int i=0; i<ecocompInfos.size(); i++)
        {
            EcocompRankInfo info = ecocompInfos.get(i);
            ArrayList<String> row = new ArrayList<String>();
            row.add(info.name);
            row.add(info.cellSum+"");
            row.add(info.maxRank+"");
            row.add(info.minRank+"");
            row.add(info.maxRank-info.minRank+"");
            row.add(info.inMostImportant25p/(0.01*simulationRuns)+"");
            row.add(info.inLeastImportant25p/(0.01*simulationRuns)+"");
            table.addRow(row);
        }
        
        table.writeToFile(new File(outputFolder,"ecocompranks.csv").getAbsolutePath());
    }


    private ArrayList<EcocompRankInfo> initializeEcocomps() 
    {
        ArrayList<EcocompRankInfo> infos=new ArrayList<>();
        for(int i =0; i<MappingProject.ecocomps.size();i++)
        {
           EcocompRankInfo newInfo = new EcocompRankInfo();
           newInfo.name = MappingProject.ecocomps.get(i).getName();
           newInfo.cellSum = MappingProject.ecocomps.get(i).getGrid().getCellSum();
           infos.add(newInfo);
        }
        return infos;
    }

    private void updateCellResults(SpatialDataLayer result) 
    {
        if(cellInfos==null) return;
        
        //fill in cellInfos with current values
        for(int i=0; i<cellInfos.size();i++)
        {
            CellRankInfo info = cellInfos.get(i);
            info.currentImpact = result.getGrid().getData()[info.x][info.y];
        }
        
        Collections.sort(cellInfos,new CellComparator());
        
        int p25 = (int) Math.round(cellInfos.size() * 0.25);
        int p10 = (int) Math.round(cellInfos.size() * 0.10);
        for(int i=0; i<cellInfos.size(); i++)
        {
            CellRankInfo info = cellInfos.get(i);
            int rank = i+1;
            if(rank<=p10) {info.inHighest10p++;}
            else if(rank>cellInfos.size()-(p10-1)) {info.inLowest10p++;}
            if(rank<=p25) {info.inHighest25p++;}
            else if(rank>cellInfos.size()-(p25-1)) {info.inLowest25p++;}
            double perc = 1-(1.0*i)/(cellInfos.size()-1);
            if(perc>info.maxPerc) {info.maxPerc=perc;}
            if(perc<info.minPerc) {info.minPerc=perc;}
        }
        
    }

    private void writeCellResults(ArrayList<CellRankInfo> cellInfos) 
    {
        double[][] maxPercentiles = MappingProject.grid.getEmptyGrid();
        double[][] minPercentiles = MappingProject.grid.getEmptyGrid();
        double[][] inTop10p = MappingProject.grid.getEmptyGrid();
        double[][] inBottom10p = MappingProject.grid.getEmptyGrid();
        double[][] inTop25p = MappingProject.grid.getEmptyGrid();
        double[][] inBottom25p = MappingProject.grid.getEmptyGrid();
    
        for(int i=0; i<cellInfos.size();i++)
        {
            CellRankInfo info = cellInfos.get(i);
            maxPercentiles[info.x][info.y] = info.maxPerc;
            minPercentiles[info.x][info.y] = info.minPerc;
            inTop10p[info.x][info.y] = info.inHighest10p/(0.01*simulationRuns);
            inBottom10p[info.x][info.y] = info.inLowest10p/(0.01*simulationRuns);
            inTop25p[info.x][info.y] = info.inHighest25p/(0.01*simulationRuns);
            inBottom25p[info.x][info.y] = info.inLowest25p/(0.01*simulationRuns);
        }
        
        DataGrid maxPGrid = new DataGrid(maxPercentiles, 1, 0, GlobalResources.NODATAVALUE);
        DataGrid minPGrid = new DataGrid(minPercentiles, 1, 0, GlobalResources.NODATAVALUE);
        DataGrid top25pGrid = new DataGrid(inTop25p, 100, 0, GlobalResources.NODATAVALUE);
        DataGrid bottom25pGrid = new DataGrid(inBottom25p, 100, 0, GlobalResources.NODATAVALUE);
        DataGrid top10pGrid = new DataGrid(inTop10p, 100, 0, GlobalResources.NODATAVALUE);
        DataGrid bottom10pGrid = new DataGrid(inBottom10p, 100, 0, GlobalResources.NODATAVALUE);
        
        DataSourceInfo maxPInfo = new DataSourceInfo();
        maxPInfo.sourceFile=new File(outputFolder, "maxp.csv").getAbsolutePath();
        maxPInfo.valueField="value";
        maxPInfo.xField="x";
        maxPInfo.yField="y";
        SpatialDataLayer maxPLayer = new SpatialDataLayer("Maximum quantiles", maxPGrid, GlobalResources.DATATYPE_SPATIAL, maxPInfo);        
        CsvTable table = MappingProject.grid.createTableFromLayer(maxPLayer, false);
        table.writeToFile(maxPInfo.sourceFile);        
        MappingProject.results.add(maxPLayer);
    
        DataSourceInfo minPInfo = new DataSourceInfo();
        minPInfo.sourceFile=new File(outputFolder, "minp.csv").getAbsolutePath();
        minPInfo.valueField="value";
        minPInfo.xField="x";
        minPInfo.yField="y";
        SpatialDataLayer minPLayer = new SpatialDataLayer("Minimum quantiles", minPGrid, GlobalResources.DATATYPE_SPATIAL, minPInfo);        
        table = MappingProject.grid.createTableFromLayer(minPLayer, false);
        table.writeToFile(minPInfo.sourceFile);        
        MappingProject.results.add(minPLayer);
    
        DataSourceInfo top25pInfo = new DataSourceInfo();
        top25pInfo.sourceFile=new File(outputFolder, "highest25p.csv").getAbsolutePath();
        top25pInfo.valueField="value";
        top25pInfo.xField="x";
        top25pInfo.yField="y";
        SpatialDataLayer top25pLayer = new SpatialDataLayer("% in highest 25%", top25pGrid, GlobalResources.DATATYPE_SPATIAL, top25pInfo);        
        table = MappingProject.grid.createTableFromLayer(top25pLayer, false);
        table.writeToFile(top25pInfo.sourceFile);        
        MappingProject.results.add(top25pLayer);
        
        DataSourceInfo bottom25pInfo = new DataSourceInfo();
        bottom25pInfo.sourceFile=new File(outputFolder, "lowest25p.csv").getAbsolutePath();
        bottom25pInfo.valueField="value";
        bottom25pInfo.xField="x";
        bottom25pInfo.yField="y";
        SpatialDataLayer bottom25pLayer = new SpatialDataLayer("% in lowest 25%", bottom25pGrid, GlobalResources.DATATYPE_SPATIAL, bottom25pInfo);        
        table = MappingProject.grid.createTableFromLayer(bottom25pLayer, false);
        table.writeToFile(bottom25pInfo.sourceFile);        
        MappingProject.results.add(bottom25pLayer);
        
        DataSourceInfo top10pInfo = new DataSourceInfo();
        top10pInfo.sourceFile=new File(outputFolder, "highest10p.csv").getAbsolutePath();
        top10pInfo.valueField="value";
        top10pInfo.xField="x";
        top10pInfo.yField="y";
        SpatialDataLayer top10pLayer = new SpatialDataLayer("% in highest 10%", top10pGrid, GlobalResources.DATATYPE_SPATIAL, top10pInfo);        
        table = MappingProject.grid.createTableFromLayer(top10pLayer, false);
        table.writeToFile(top10pInfo.sourceFile);        
        MappingProject.results.add(top10pLayer);
        
        
        DataSourceInfo bottom10pInfo = new DataSourceInfo();
        bottom10pInfo.sourceFile=new File(outputFolder, "lowest10p.csv").getAbsolutePath();
        bottom10pInfo.valueField="value";
        bottom10pInfo.xField="x";
        bottom10pInfo.yField="y";
        SpatialDataLayer bottom10pLayer = new SpatialDataLayer("% in lowest 10%", bottom10pGrid, GlobalResources.DATATYPE_SPATIAL, bottom10pInfo);        
        table = MappingProject.grid.createTableFromLayer(bottom10pLayer, false);
        table.writeToFile(bottom10pInfo.sourceFile);        
        MappingProject.results.add(bottom10pLayer);
        
    }

public StressorRankInfo getStressorInfoByName(String name) 
    {
        StressorRankInfo info=null;
        if(stressorInfos==null) {return null;}
        for(int i=0; i<stressorInfos.size();i++)
        {
            if(stressorInfos.get(i).name.equals(name)) {info=stressorInfos.get(i);}
        }
        return info;
    }

    void mergeResults(MCSimulationManager mcm2) 
    {
        //for stressors
        for(int i=0; i<stressorInfos.size();i++)
        {
            for(int j=0; j<mcm2.stressorInfos.size(); j++)
            {
                if(stressorInfos.get(i).name.equals(mcm2.stressorInfos.get(j).name))
                {
                    stressorInfos.get(i).inLeastImportant25p = stressorInfos.get(i).inLeastImportant25p + mcm2.stressorInfos.get(j).inLeastImportant25p;
                    stressorInfos.get(i).inMostImportant25p = stressorInfos.get(i).inMostImportant25p + mcm2.stressorInfos.get(j).inMostImportant25p;
                    stressorInfos.get(i).included = stressorInfos.get(i).included + mcm2.stressorInfos.get(j).included;
                    if(mcm2.stressorInfos.get(j).maxRank>stressorInfos.get(i).maxRank) {stressorInfos.get(i).maxRank=stressorInfos.get(j).maxRank;}
                    if(mcm2.stressorInfos.get(j).minRank<stressorInfos.get(i).minRank) {stressorInfos.get(i).minRank=stressorInfos.get(j).minRank;}
                }
            }
        }
        
        //for ecocomps
        for(int i=0; i<ecocompInfos.size();i++)
        {
            for(int j=0; j<mcm2.ecocompInfos.size(); j++)
            {
                if(ecocompInfos.get(i).name.equals(mcm2.ecocompInfos.get(j).name))
                {
                    ecocompInfos.get(i).inLeastImportant25p = ecocompInfos.get(i).inLeastImportant25p + mcm2.ecocompInfos.get(j).inLeastImportant25p;
                    ecocompInfos.get(i).inMostImportant25p = ecocompInfos.get(i).inMostImportant25p + mcm2.ecocompInfos.get(j).inMostImportant25p;   
                    if(mcm2.ecocompInfos.get(j).maxRank>ecocompInfos.get(i).maxRank) {ecocompInfos.get(i).maxRank=ecocompInfos.get(j).maxRank;}
                    if(mcm2.ecocompInfos.get(j).minRank<ecocompInfos.get(i).minRank) {ecocompInfos.get(i).minRank=ecocompInfos.get(j).minRank;}
                }
            }
        }
        
        //for regions
        for(int i=0; i<regionInfos.size();i++)
        {
            for(int j=0; j<mcm2.regionInfos.size(); j++)
            {
                if(regionInfos.get(i).regionCode==mcm2.regionInfos.get(j).regionCode)
                {
                    regionInfos.get(i).inTop25p = regionInfos.get(i).inTop25p + mcm2.regionInfos.get(j).inTop25p;
                    regionInfos.get(i).inBottom25p = regionInfos.get(i).inBottom25p + mcm2.regionInfos.get(j).inBottom25p;   
                    if(mcm2.regionInfos.get(j).maxRank>regionInfos.get(i).maxRank) {regionInfos.get(i).maxRank=regionInfos.get(j).maxRank;}
                    if(mcm2.regionInfos.get(j).minRank<regionInfos.get(i).minRank) {regionInfos.get(i).minRank=regionInfos.get(j).minRank;}
                }
            }
        }        
        
        //for spatial results
        if(createSpatialOutputs)
        {
            for(int i=0; i<cellInfos.size();i++)
            {
                for(int j=0; j<mcm2.cellInfos.size(); j++)
                {
                    if(cellInfos.get(i).x == mcm2.cellInfos.get(j).x && cellInfos.get(i).y == mcm2.cellInfos.get(j).y)
                    {
                        cellInfos.get(i).inHighest10p = cellInfos.get(i).inHighest10p + mcm2.cellInfos.get(j).inHighest10p;
                        cellInfos.get(i).inLowest10p = cellInfos.get(i).inLowest10p + mcm2.cellInfos.get(j).inLowest10p;  
                        cellInfos.get(i).inHighest25p = cellInfos.get(i).inHighest25p + mcm2.cellInfos.get(j).inHighest25p;
                        cellInfos.get(i).inLowest25p = cellInfos.get(i).inLowest25p + mcm2.cellInfos.get(j).inLowest25p;  
                        if(mcm2.cellInfos.get(j).maxPerc>cellInfos.get(i).maxPerc) {cellInfos.get(i).maxPerc=cellInfos.get(j).maxPerc;}
                        if(mcm2.cellInfos.get(j).minPerc<cellInfos.get(i).minPerc) {cellInfos.get(i).minPerc=cellInfos.get(j).maxPerc;}
                    }
                }
            } 
        }
        
        simulationRuns+=mcm2.simulationRuns;
        
    }

}