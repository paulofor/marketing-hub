package com.marketinghub.geralanding;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WireframeHtmlGenerator {

    public String generateFromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        String sectionOrder = extractSectionOrderArray(json);
        List<String> tags = extractStringFieldValues(sectionOrder, "uiTags");
        List<String> sizeBlocks = extractStringFieldValues(sectionOrder, "uiSizes");

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>Wireframe provisório</title>\n<style>\n");
        html.append("*{box-sizing:border-box;} body{margin:0;font-family:Arial,sans-serif;line-height:1.4;color:#111;} img{display:block;} section{border-bottom:1px solid #eee;}\n");
        html.append("#s1-form{background:#fde68a;color:#1f2937;border:2px solid #f59e0b;} #lead-form{background:#fff7ed;padding:12px;border-radius:10px;}\n");
        for (String sizeJson : sizeBlocks) html.append(toCss(sizeJson)).append("\n");
        html.append("</style>\n</head>\n<body>\n");

        int sectionIndex = 0;
        for (String tag : tags) {
            String sectionHtml = applyAlternatingSectionStyle(tag, sectionIndex++);
            sectionHtml = fillTextPlaceholders(sectionHtml);
            sectionHtml = colorizeImageSlots(sectionHtml);
            html.append(sectionHtml).append("\n");
        }

        html.append("</body>\n</html>\n");
        return html.toString();
    }

    private static String extractSectionOrderArray(String json) {int keyIndex=json.indexOf("\"sectionOrder\"");if(keyIndex<0)throw new IllegalArgumentException();int open=json.indexOf('[',keyIndex);int d=0;boolean s=false;for(int i=open;i<json.length();i++){char c=json.charAt(i);if(c=='\"'&&json.charAt(i-1)!='\\')s=!s;if(!s){if(c=='[')d++;else if(c==']'){d--;if(d==0)return json.substring(open,i+1);}}}throw new IllegalArgumentException();}
    private static List<String> extractStringFieldValues(String text, String field){Pattern p=Pattern.compile("\\\""+Pattern.quote(field)+"\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");Matcher m=p.matcher(text);List<String>v=new ArrayList<>();while(m.find())v.add(unescapeJson(m.group(1)));return v;}
    private static String unescapeJson(String raw){return raw.replace("\\\"","\"").replace("\\n","\n").replace("\\r","").replace("\\t","\t").replace("\\/","/").replace("\\\\","\\");}
    private static String toCss(String uiSizesJson){String n=uiSizesJson.trim();if(!StringUtils.hasText(n))return "";if(looksLikePlainCss(n))return n;StringBuilder css=new StringBuilder();int i=0;while(i<n.length()){if(n.charAt(i)=='\"'){int se=findStringEnd(n,i+1);String sel=n.substring(i+1,se);i=n.indexOf('{',se);if(i<0)break;int oe=findMatchingBrace(n,i);String ro=n.substring(i+1,oe);if(sel.startsWith("@media"))css.append(sel).append("{\n").append(parseNestedRules(ro)).append("}\n");else css.append(sel).append(" {\n").append(parseDeclarations(ro)).append("}\n");i=oe+1;}else i++;}return css.toString();}
    private static String parseNestedRules(String nestedObj){StringBuilder o=new StringBuilder();int i=0;while(i<nestedObj.length()){if(nestedObj.charAt(i)=='\"'){int e=findStringEnd(nestedObj,i+1);String s=nestedObj.substring(i+1,e);i=nestedObj.indexOf('{',e);int oe=findMatchingBrace(nestedObj,i);String d=nestedObj.substring(i+1,oe);o.append(s).append(" {\n").append(parseDeclarations(d)).append("}\n");i=oe+1;}else i++;}return o.toString();}
    private static String parseDeclarations(String rulesObj){Pattern p=Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");Matcher m=p.matcher(rulesObj);StringBuilder o=new StringBuilder();while(m.find())o.append("  ").append(toKebabCase(m.group(1))).append(": ").append(m.group(2)).append(";\n");return o.toString();}
    private static boolean looksLikePlainCss(String value){return value.contains("#")&&value.contains("{")&&value.contains("}")&&!value.contains("\"#");}
    private static String toKebabCase(String s){return s.replaceAll("([a-z])([A-Z])","$1-$2").toLowerCase();}
    private static int findStringEnd(String s,int st){for(int i=st;i<s.length();i++)if(s.charAt(i)=='\"'&&s.charAt(i-1)!='\\')return i;throw new IllegalArgumentException();}
    private static int findMatchingBrace(String s,int open){int d=0;boolean t=false;for(int i=open;i<s.length();i++){char c=s.charAt(i);if(c=='\"'&&s.charAt(i-1)!='\\')t=!t;if(!t){if(c=='{')d++;else if(c=='}'){d--;if(d==0)return i;}}}throw new IllegalArgumentException();}
    private static String applyAlternatingSectionStyle(String h,int idx){String background=idx%2==0?"#ffffff":"#111827";String textColor=idx%2==0?"#111827":"#f9fafb";String borderColor=idx%2==0?"#e5e7eb":"#374151";String sectionStyle="background:"+background+";color:"+textColor+";border-bottom:1px solid "+borderColor+";";return h.replaceAll("<section(\\s|>)","<section style=\""+sectionStyle+"\"$1");}
    private static String fillTextPlaceholders(String h){String t=h.replaceAll("<(h1|h2|h3)([^>]*)></\\1>","<$1$2>Lorem ipsum dolor sit amet</$1>");String p=t.replaceAll("<p([^>]*)></p>","<p$1>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>");String sp=p.replaceAll("<span([^>]*)></span>","<span$1>Lorem ipsum</span>");String su=sp.replaceAll("<summary([^>]*)></summary>","<summary$1>Lorem ipsum dolor sit amet?</summary>");String li=su.replaceAll("<li([^>]*)></li>","<li$1>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</li>");String a=li.replaceAll("<a([^>]*)></a>","<a$1 href=\"#lead-form\">Lorem ipsum dolor sit amet</a>");String b=a.replaceAll("<button([^>]*)></button>","<button$1 type=\"button\">Lorem ipsum dolor sit amet</button>");return b.replaceAll("<img([^>]*)/>","<img$1 alt=\"Lorem ipsum preview\" src=\"https://via.placeholder.com/800x500?text=Lorem+Ipsum\" />");}
    private static String colorizeImageSlots(String h){String[] p={"#dbeafe","#dcfce7","#fee2e2","#ede9fe","#fef3c7"};Matcher m=Pattern.compile("<img([^>]*)/>").matcher(h);StringBuffer o=new StringBuffer();while(m.find()){String attrs=m.group(1);String id=extractId(attrs);String bg=p[Math.abs(id.hashCode())%p.length];String styleAddon="background:"+bg+";border:2px dashed #475569;padding:8px;border-radius:12px;";Matcher styleMatcher=Pattern.compile("\\sstyle=\"([^\"]*)\"").matcher(attrs);String mergedAttrs;if(styleMatcher.find()){String existing=styleMatcher.group(1).trim();String mergedStyle=existing.endsWith(";")?existing+styleAddon:existing+";"+styleAddon;mergedAttrs=styleMatcher.replaceFirst(" style=\""+Matcher.quoteReplacement(mergedStyle)+"\"");}else{mergedAttrs=attrs+" style=\""+styleAddon+"\"";}m.appendReplacement(o,Matcher.quoteReplacement("<img"+mergedAttrs+"/>"));}m.appendTail(o);return o.toString();}
    private static String extractId(String a){Matcher m=Pattern.compile("id=\"([^\"]+)\"").matcher(a);return m.find()?m.group(1):"img";}
}
