package ru.rexchange.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtils {
	public static final String DEFAULT_ENCODING = "UTF-8";

	public static String readToString(InputStream is, String encoding, int maxSize)
			throws IOException {
		char[] buf = new char[4096];

    try (BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding))) {
      StringBuilder sb = new StringBuilder();

      do {
        int c = in.read(buf);
        if (c < 0) {
          return sb.toString();
        }

        sb.append(buf, 0, c);
      } while (maxSize <= 0 || sb.length() < maxSize);

      sb.setLength(maxSize);
      return sb.toString();
    }
	}

	public static String readToString(InputStream is, String encoding) throws IOException {
		return readToString((InputStream) is, encoding, -1);
	}

}
