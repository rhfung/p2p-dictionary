package com.rhfung.P2PDictionary;

import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by richard on 1/18/16.
 */
public class URLUtils {
    /**
     * Removes extra backslash.
     * @param readableURL
     * @return
     */
    public static String URLEncode(String readableURL)
    {
        if (readableURL == null) {
            return readableURL;
        }
        if (readableURL.equals("/")) {
            return readableURL;
        }
        try
        {
            String[] parts = readableURL.split("/");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = URLEncoder.encode(parts[i], "utf-8");
            }
            return  StringUtils.join(parts, "/");
        }
        catch(Exception ex)
        {
            return readableURL;
        }
    }

    /**
     * Removes extra backslash.
     * @param encodedURL
     * @return
     */
    public static String URLDecode(String encodedURL)
    {
        if (encodedURL == null) {
            return encodedURL;
        }
        if (encodedURL.equals("/")) {
            return encodedURL;
        }
        try
        {
            String[] parts = encodedURL.split("/");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = URLDecoder.decode(parts[i], "utf-8");
            }
            return StringUtils.join(parts, "/");
        }
        catch(Exception ex)
        {
            return encodedURL;
        }
    }

}
