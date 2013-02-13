package edu.columbia.ldpd.hrwa.util.common;

import java.net.MalformedURLException;
import java.net.URL;

//TODO: Consolidate with StringUtils class in hrwa_asf-manager when this project gets folded into that repo

public class MetadataUtils {

	public static String parseHoststring( String url ) throws MalformedURLException {
		URL urlobj = new URL( url );
    	String hoststring = urlobj.getHost().replaceFirst( "\\Awww[\\w]?\\.", "" );
    	
    	return hoststring;
    }

}
