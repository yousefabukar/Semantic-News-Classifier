package uob.oop;

public class HtmlParser {
    /***
     * Extract the title of the news from the _htmlCode.
     * @param _htmlCode Contains the full HTML string from a specific news. E.g. 01.htm.
     * @return Return the title if it's been found. Otherwise, return "Title not found!".
     */
    public static String getNewsTitle(String _htmlCode) {
        String titleTagOpen = "<title>";
        String titleTagClose = "</title>";

        int titleStart = _htmlCode.indexOf(titleTagOpen) + titleTagOpen.length();
        int titleEnd = _htmlCode.indexOf(titleTagClose);

        if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) {
            String strFullTitle = _htmlCode.substring(titleStart, titleEnd);
            return strFullTitle.substring(0, strFullTitle.indexOf(" |"));
        }

        return "Title not found!";
    }

    /***
     * Extract the content of the news from the _htmlCode.
     * @param _htmlCode Contains the full HTML string from a specific news. E.g. 01.htm.
     * @return Return the content if it's been found. Otherwise, return "Content not found!".
     */
    public static String getNewsContent(String _htmlCode) {
        String contentTagOpen = "\"articleBody\": \"";
        String contentTagClose = " \",\"mainEntityOfPage\":";

        int contentStart = _htmlCode.indexOf(contentTagOpen) + contentTagOpen.length();
        int contentEnd = _htmlCode.indexOf(contentTagClose);

        if (contentStart != -1 && contentEnd != -1 && contentEnd > contentStart) {
            return _htmlCode.substring(contentStart, contentEnd).toLowerCase();
        }

        return "Content not found!";
    }

    public static NewsArticles.DataType getDataType(String _htmlCode) {
        String openDatatype = "<datatype>";
        String closeDatatype = "</datatype>";
        int datatypeStart = _htmlCode.indexOf(openDatatype) + openDatatype.length();
        int datatypeEnd = _htmlCode.indexOf(closeDatatype);

        if (datatypeEnd - datatypeStart == 8) {
            return NewsArticles.DataType.Training;
        } else {
            return NewsArticles.DataType.Testing;
        }
    }

    public static String getLabel (String _htmlCode) {
        String openLabel = "<label>";
        String closeLabel = "</label>";
        int labelStart = _htmlCode.indexOf(openLabel) + openLabel.length();
        int labelClose = _htmlCode.indexOf(closeLabel);

        if (labelStart != -1 && labelClose != -1) {
            String finalLabel = _htmlCode.substring(labelStart, labelClose);
            return finalLabel;
        } else {
            return "-1";
        }
    }
}
