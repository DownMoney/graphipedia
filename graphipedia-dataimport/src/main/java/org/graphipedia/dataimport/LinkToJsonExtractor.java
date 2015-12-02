//
// Copyright (c) 2012 Mirko Nasato
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
// OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.
//
package org.graphipedia.dataimport;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class LinkToJsonExtractor extends SimpleStaxParser {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(.+?)\\]\\]");

    private final ProgressCounter pageCounter = new ProgressCounter();
    private PrintWriter csv;

    private String title;
    private String text;

    private Integer counter = 0;
    private Map<String, Integer> mapping;

    private PrintWriter names;

    public LinkToJsonExtractor(PrintWriter csv, PrintWriter names) {
        super(Arrays.asList("page", "title", "text"));

        this.csv = csv;
        mapping = new HashMap<String, Integer>();
        this.names = names;
    }

    public int getPageCount() {
        return pageCounter.getCount();
    }

    @Override
    protected void handleElement(String element, String value) {
        if ("page".equals(element)) {
            if (!title.contains(":")) {
                try {
                    writePage(title, text);
                } catch (XMLStreamException streamException) {
                    throw new RuntimeException(streamException);
                }
            }
            title = null;
            text = null;
        } else if ("title".equals(element)) {
            title = value;
        } else if ("text".equals(element)) {
            text = value;
        }
    }

    private Integer getMapping(String text) {
        if (!mapping.containsKey(text)) {
            Integer oldCounter = counter;
            mapping.put(text, counter);
            names.println(counter.toString() + "," + text.replace(",", ""));
            counter++;
            return oldCounter;
        } else {
            return mapping.get(text);
        }
    }

    private void writePage(String title, String text) throws XMLStreamException {
        String line = getMapping(title).toString() + ":[";


        Set<String> links = parseLinks(text);
        links.remove(title);

        for (String link : links) {
            line += getMapping(link).toString() + ",";
        }

        if (links.size() > 0) {
            line = line.substring(0, line.length() - 1);
        }

        line += "],";
        csv.println(line);
        pageCounter.increment();
    }

    private Set<String> parseLinks(String text) {
        Set<String> links = new HashSet<String>();
        if (text != null) {
            Matcher matcher = LINK_PATTERN.matcher(text);
            while (matcher.find()) {
                String link = matcher.group(1);
                if (!link.contains(":")) {
                    if (link.contains("|")) {
                        link = link.substring(0, link.lastIndexOf('|'));
                    }
                    links.add(link);
                }
            }
        }
        return links;
    }

}
