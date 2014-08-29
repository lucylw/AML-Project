/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* Heuristic repair algorithm that approximates the global minimum number of   *
* removed mappings.                                                           *
*                                                                             *
* @author Daniel Faria & Emanuel Santos                                       *
* @date 10-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import java.util.HashSet;
import java.util.Vector;

import aml.AML;
import aml.match.Alignment;
import aml.ontology.URIMap;
import aml.util.Oracle;
import aml.util.Table2List;

public class InteractiveRepairer implements Repairer
{
	
//Attributes
	
	private Alignment toRepair;
	private Vector<Path> conflictSets;
	private HashSet<Integer> correct;
	private Table2List<Integer,Integer> conflictMappings;
	private Table2List<Integer,Integer> mappingConflicts;
	
//Constructors

	/**
	 * Constructs a new InteractiveRepairer
	 */
	public InteractiveRepairer(){}
	
//Public Methods
	
	@Override
	public Alignment repair(Alignment a)
	{
		toRepair = a;
		AML aml = AML.getInstance();
		URIMap map = aml.getURIMap();
		RepairMap rMap = new RepairMap(a);
		conflictSets = rMap.getConflictSets();
		init();
		if(conflictSets.size() == 0)
			return new Alignment(toRepair);
		System.out.println("Repairing Alignment Interactively");
		long time = System.currentTimeMillis()/1000;
		HashSet<Integer> toRemove = new HashSet<Integer>();
		while(conflictMappings.size() > 0)
		{
			int worstMapping = getWorstMapping();
			String sourceURI = map.getURI(toRepair.get(worstMapping).getSourceId());
			String targetURI = map.getURI(toRepair.get(worstMapping).getTargetId());

			if(Oracle.check(sourceURI,targetURI,toRepair.get(worstMapping).getRelationship()))
				correct.add(worstMapping);
			else
			{
				toRemove.add(worstMapping);
				remove(worstMapping);
			}
			if(correct.containsAll(conflictMappings.keySet()))
				break;
		}
		Alignment repaired = new Alignment();
		for(int i = 0; i < toRepair.size(); i++)
		{
			if(!toRemove.contains(i))
				repaired.add(toRepair.get(i));
		}
		System.out.println("Finished Repair in " + 
				(System.currentTimeMillis()/1000-time) + " seconds");
		System.out.println("Removed " + toRemove.size() + " mappings");
		return repaired;
	}
	
//Private Methods
	
	private int getWorstMapping()
	{
		int worstMapping = -1;
		int maxCard = 0;
		
		for(Integer i : mappingConflicts.keySet())
		{
			if(correct.contains(i))
				continue;
			int card = mappingConflicts.get(i).size();
			if(card > maxCard || (card == maxCard && toRepair.get(i).getSimilarity() <
					toRepair.get(worstMapping).getSimilarity()))
			{
				maxCard = card;
				worstMapping = i;
			}
		}
		return worstMapping;
	}
	
	private void init()
	{
		conflictMappings = new Table2List<Integer,Integer>();
		mappingConflicts = new Table2List<Integer,Integer>();
		correct = new HashSet<Integer>();
		for(int i = 0; i < conflictSets.size(); i++)
		{
			for(Integer j : conflictSets.get(i))
			{
				conflictMappings.add(i,j);
				mappingConflicts.add(j,i);
			}
		}
	}
	
	private void remove(int mapping)
	{
		HashSet<Integer> conflicts = new HashSet<Integer>(mappingConflicts.get(mapping));
		for(Integer i : conflicts)
		{
			for(Integer j : conflictMappings.get(i))
			{
				if(mappingConflicts.get(j).size() == 1)
					mappingConflicts.remove(j);
				else
					mappingConflicts.remove(j,i);
			}
			conflictMappings.remove(i);
		}
		mappingConflicts.remove(mapping);
	}
}