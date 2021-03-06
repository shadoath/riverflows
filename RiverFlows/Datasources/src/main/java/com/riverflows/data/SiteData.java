package com.riverflows.data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.riverflows.data.Variable.CommonVariable;

public class SiteData {
	
	public static final Comparator<SiteData> SORT_BY_SITE_NAME = new Comparator<SiteData>() {
		@Override
		public int compare(SiteData object1, SiteData object2) {
			if(object1 == null || object1.getSite() == null || object1.getSite().getName() == null) {
				return -1;
			}
			if(object2 == null || object2.getSite() == null || object2.getSite().getName() == null) {
				return 1;
			}
			return object1.getSite().getName().toUpperCase().compareTo(object2.getSite().getName().toUpperCase());
		}
	};
	
	private Site site;
	private Map<CommonVariable,Series> datasets = new HashMap<CommonVariable, Series>();
	private String dataInfo;

    private boolean complete = true;

	/**
	 * return datasets variable type -> series mappings
	 */
	public Map<CommonVariable, Series> getDatasets() {
		return datasets;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}
	
	@Override
	public String toString() {
		return site.toString();
	}

	/**
	 * @return HTML snippet describing this data
	 */
	public String getDataInfo() {
		return dataInfo;
	}

	public void setDataInfo(String dataInfo) {
		this.dataInfo = dataInfo;
	}

    /**
     * @return true if this includes all available datasets at this site
     */
    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
