package extdoc.jsdoc.processor;

import extdoc.jsdoc.docs.*;
import extdoc.jsdoc.tags.*;
import extdoc.jsdoc.tags.impl.Comment;
import extdoc.jsdoc.tplschema.*;
import extdoc.jsdoc.tree.TreePackage;
import extdoc.jsdoc.util.StringUtils;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;


/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 4:41:12
 */
public class FileProcessor{

    private static Logger logger = Logger.getLogger("extdoc.jsdoc.processor");

    public List<DocClass> classes = new ArrayList<DocClass>();
    public List<DocCfg> cfgs = new ArrayList<DocCfg>();
    public List<DocProperty> properties =
            new ArrayList<DocProperty>();
    public List<DocMethod> methods = new ArrayList<DocMethod>();
    public List<DocEvent> events = new ArrayList<DocEvent>();

    private TreePackage tree = new TreePackage();

    private final static String OUT_FILE_EXTENSION = "html";
    private final static boolean GENERATE_DEBUG_XML = false;
    private final static String COMPONENT_NAME =
            "Ext.Component";
    private final static String DEFAULT_TYPE = "Object";

    private String className;
    private String shortClassName;
    private String currFile;

    private static final String START_LINK = "{@link";    

    private static enum LinkStates {READ, LINK}

    private static final String
        MEMBER_REFERENCE_TPL =
            "<a href=\"output/{0}.html#{1}\" " +
                    "ext:member=\"{1}\" ext:cls=\"{0}\">{2}</a>";

    private static final String
        CLASS_REFERENCE_TPL =
            "<a href=\"output/{0}.html\" " +
                    "ext:cls=\"{0}\">{1}</a>";

    private static final int DESCR_MAX_LENGTH = 117;

    /**
     * Processes link content (between "{" and "}")
     * @param text Content, ex: "Ext.DomQuery#select"
     * @return Array of 2 Strings: long and short versions
     */
    private String[] processLink(String text){
        StringUtils.ClsAttrName res = StringUtils.processLink(text);
        String longText, shortText;
        if (res.attr.isEmpty()){
           // class reference
            String cls = res.cls;
            String name = res.name.isEmpty()?res.cls:res.name;
            longText =
                    MessageFormat.format(CLASS_REFERENCE_TPL, cls, name);
            shortText = name;
        }else{
           // attribute reference
            String cls = res.cls.isEmpty()?className:res.cls;
            String attr = res.attr;
            String name;
            if (res.name.isEmpty()){
                if (res.cls.isEmpty()){
                    name = res.attr;
                }else{
                    name = cls + '.' + res.attr;
                }
            }else{
                name = res.name;
            }
            longText =
                    MessageFormat.format(
                            MEMBER_REFERENCE_TPL, cls, attr, name);
            shortText = name;
        }
        return new String[]{longText, shortText};
    }

    private Description inlineLinks(String content){
        return inlineLinks(content, false);
    }    

    /**
     * Replaces inline tag @link to actual html links and returns shot and/or
     *  long versions.
     * @param cnt description content
     * @param alwaysGenerateShort forces to generate short version for
     * Methods and events
     * @return short and long versions
     */
    private Description inlineLinks(String cnt,
                                                                boolean alwaysGenerateShort){

        if (cnt==null) return null;
        String content = StringUtils.highlightCode(cnt);
        LinkStates state = LinkStates.READ;
        StringBuilder sbHtml = new StringBuilder();
        StringBuilder sbText = new StringBuilder();
        StringBuilder buffer = new StringBuilder();        
        for (int i=0;i<content.length();i++){
            char ch = content.charAt(i);
            switch (state){
                case READ:
                    if (StringUtils.endsWith(buffer, START_LINK)){
                        String substr = buffer.substring(
                                            0, buffer.length() - START_LINK.length());
                        sbHtml.append(substr);
                        sbText.append(substr);
                        buffer.setLength(0);
                        state = LinkStates.LINK;
                        break;
                    }
                    buffer.append(ch);
                    break;
                case LINK:
                    if(ch=='}'){
                        String[] str = processLink(buffer.toString()); 
                        sbHtml.append(str[0]);
                        sbText.append(str[1]);
                        buffer.setLength(0);
                        state = LinkStates.READ;
                        break;
                    }
                    buffer.append(ch);
                    break;
            }
        }

        // append remaining
        sbHtml.append(buffer);
        sbText.append(buffer);

        String sbString = sbText.toString().replaceAll("<\\S*?>","");        

        Description description = new Description();
        description.longDescr =  sbHtml.toString();
        if(alwaysGenerateShort){
            description.hasShort = true;
            description.shortDescr =
                sbString.length()>DESCR_MAX_LENGTH?
                        new StringBuilder()
                            .append(sbString.substring(0, DESCR_MAX_LENGTH))
                            .append("...").toString()
                :sbString;
        }else{
            description.hasShort = sbString.length()>DESCR_MAX_LENGTH;
            description.shortDescr =
                description.hasShort?
                        new StringBuilder()
                            .append(sbString.substring(0, DESCR_MAX_LENGTH))
                            .append("...").toString()
                :null;
        }
        return description;
    }


    /**
     *  Read params from list of param tags and add them to list of params
     *  Just simplifies param processing for class, method and event
     * @param paramTags tags
     * @param  params target list of params
     */
    private void readParams(List<ParamTag> paramTags,
                                                        List<Param> params){
        for(ParamTag paramTag: paramTags){
            Param param = new Param();
            param.name = paramTag.getParamName();
            param.type = paramTag.getParamType();
            Description descr = inlineLinks(paramTag.getParamDescription());
            param.description = descr!=null?descr.longDescr:null;
            param.optional = paramTag.isOptional();
            params.add(param);
        }
    }


   

    /**
     * Process class 
     * @param comment Comment
     */
    private void processClass(Comment comment){

        DocClass cls = new DocClass();
        
        ClassTag classTag = comment.tag("@class");
        Tag singletonTag = comment.tag("@singleton");
        ExtendsTag extendsTag = comment.tag("@extends");
        Tag constructorTag = comment.tag("@constructor");
        List<ParamTag> paramTags = comment.tags("@param");

        cls.className = classTag.getClassName();
        String[] str = StringUtils.separatePackage(cls.className);
        cls.packageName = str[0];
        cls.shortClassName = str[1];
        cls.definedIn = currFile;
        cls.singleton = singletonTag!=null;
        String description = classTag.getClassDescription();
        if (description==null && extendsTag!=null){
            description = extendsTag.getClassDescription();
        }
        Description descr = inlineLinks(description);
        cls.description = descr!=null?descr.longDescr:null;
        cls.parentClass =
                (extendsTag!=null)?extendsTag.getClassName():null;
        cls.hasConstructor = constructorTag!=null;
        if (constructorTag!=null){
            cls.constructorDescription = inlineLinks(constructorTag.text(), true);
            readParams(paramTags, cls.params);
        }

        // Skip private classes
        if (!comment.hasTag("@private")
                && !comment.hasTag("@ignore")) {
            classes.add(cls);
        }
        className = cls.className;
        shortClassName = cls.shortClassName;

        // Process cfg declared inside class definition
        // goes after global className set
        List<CfgTag> innerCfgs =  comment.tags("@cfg");
        for (CfgTag innerCfg: innerCfgs){
            DocCfg cfg = getDocCfg(innerCfg);
            cfgs.add(cfg);
        }
        
    }

    /**
     * Helper method to process cfg in separate comment and in class
     * definition
     */
    private DocCfg getDocCfg(CfgTag tag){
        DocCfg cfg = new DocCfg();
        cfg.name = tag.getCfgName();
        cfg.type = tag.getCfgType();
        cfg.description = inlineLinks(tag.getCfgDescription());
        cfg.optional = tag.isOptional();
        cfg.className = className;
        cfg.shortClassName = shortClassName;
        return cfg;
    }

    /**
     * Process cfg
     * @param comment Comment
     */
    private void processCfg(Comment comment){
        // Skip private
        if (comment.hasTag("@private")
                || comment.hasTag("@ignore")) return;
        CfgTag tag = comment.tag("@cfg");
        DocCfg cfg = getDocCfg(tag);
        cfg.hide = comment.tag("@hide")!=null;
        cfgs.add(cfg);
    }

    /**
     * Process property 
     * @param comment Comment
     * @param extraLine first word form the line after comment
     */
    private void processProperty(Comment comment,String extraLine){
        // Skip private
        if (comment.hasTag("@private")
                || comment.hasTag("@ignore")) return;

        
        DocProperty property = new DocProperty();

        PropertyTag propertyTag = comment.tag("@property");
        TypeTag typeTag = comment.tag("@type");

        property.name = StringUtils.separateByLastDot(extraLine)[1];
        String description = comment.getDescription();
        if (propertyTag!=null){
            String propertyName = propertyTag.getPropertyName();
            if (propertyName!=null && propertyName.length()>0){
                property.name = propertyName;    
            }
            String propertyDescription = propertyTag.getPropertyDescription();
            if (propertyDescription!=null && propertyDescription.length()>0){
                description = propertyDescription;
            }
        }
        property.type = typeTag!=null?typeTag.getType():DEFAULT_TYPE;
        property.description = inlineLinks(description);
        property.className = className;
        property.shortClassName = shortClassName;
        property.hide = comment.tag("@hide")!=null;
        properties.add(property);
    }

    /**
     * Process method 
     * @param comment Comment
     * @param extraLine first word form the line after comment
     */
    private void processMethod(Comment comment, String extraLine){
        // Skip private
        if (comment.hasTag("@private")
                || comment.hasTag("@ignore")) return;


        DocMethod method = new DocMethod();

        Tag methodTag = comment.tag("@method");
        Tag staticTag = comment.tag("@static");
        List<ParamTag> paramTags = comment.tags("@param");
        ReturnTag returnTag = comment.tag("@return");
        MemberTag memberTag = comment.tag("@member");

        // should be first because @member may redefine class
        method.className = className;
        method.shortClassName = shortClassName;
        method.name = StringUtils.separatePackage(extraLine)[1];
        if (methodTag!=null){
            if (!methodTag.text().isEmpty()){
                method.name = methodTag.text();
            }
        }
        if (memberTag!=null){
            String name = memberTag.getMethodName();
            if (name!=null){
                method.name = name;
            }
            method.className = memberTag.getClassName();
            method.shortClassName =
                    StringUtils.separatePackage(method.className)[1];
        }
        method.isStatic = (staticTag!=null);

        // renaming if static
//        if(method.isStatic){
//            method.name = new StringBuilder()
//                    .append(shortClassName)
//                    .append('.')
//                    .append(separateByLastDot(extraLine)[1])
//                    .toString();
//        }

        method.description = inlineLinks(comment.getDescription(), true);
        if (returnTag!=null){
            method.returnType =returnTag.getReturnType();
            method.returnDescription =returnTag.getReturnDescription();
        }
        readParams(paramTags, method.params);
        method.hide = comment.tag("@hide")!=null;
        methods.add(method);
    }

    /**
     * Process event
     * @param comment Comment
     */
    private void processEvent(Comment comment){
        // Skip private
        if (comment.hasTag("@private")
                || comment.hasTag("@ignore")) return;

        
        DocEvent event = new DocEvent();
        EventTag eventTag = comment.tag("@event");
        List<ParamTag> paramTags = comment.tags("@param");
        event.name = eventTag.getEventName();
        event.description = inlineLinks(eventTag.getEventDescription(), true);
        readParams(paramTags, event.params);
        event.className = className;
        event.shortClassName = shortClassName;
        event.hide = comment.tag("@hide")!=null;
        events.add(event);
    }

    enum CommentType{
        CLASS, CFG, PROPERTY, METHOD, EVENT}

    static CommentType resolveCommentType(Comment comment){
        return resolveCommentType(comment, "", "");
    }

    static CommentType resolveCommentType(Comment comment, String extraLine, String extra2Line){
        if(comment.hasTag("@class")){
            return CommentType.CLASS;
        }else if(comment.hasTag("@event")){
            return CommentType.EVENT;
        }else if(comment.hasTag("@cfg")){
            return CommentType.CFG;
        }else if(comment.hasTag("@param")
                || comment.hasTag("@return")
                || comment.hasTag("@method")){
            return CommentType.METHOD;
        }else if (comment.hasTag("@type")
                || comment.hasTag("@property")){
            return CommentType.PROPERTY;                    
        }else if(extra2Line.equals("function")){
            return CommentType.METHOD;
        }else{
            return CommentType.PROPERTY;
        }
    }


    /**
     *  Determine type of comment and process it
     * @param content text inside / ** and * /
     * @param extraLine first word form the line after comment 
     */
    private void processComment(String content, String extraLine, String extra2Line){
        if (content==null) return;
        Comment comment = new Comment(content);
        switch (resolveCommentType(comment, extraLine, extra2Line)){
            case CLASS:
                processClass(comment);
                break;
            case CFG:
                processCfg(comment);
                break;
            case PROPERTY:
                processProperty(comment, extraLine);
                break;
            case METHOD:
                processMethod(comment, extraLine);
                break;
            case EVENT:
                processEvent(comment);
                break;
        }
    }

    private enum State {CODE, COMMENT}
    private enum ExtraState {SKIP, SPACE, READ, SPACE2, READ2}

    private static final String START_COMMENT = "/**";
    private static final String END_COMMENT = "*/";


    /**
     * Checks if char is white space in terms of extra line of code after
     * comments
     * @param ch character
     * @return true if space or new line or * or / or ' etc...
     */
    private boolean isWhite(char ch){
        return !Character.isLetterOrDigit(ch) && ch!='.' && ch!='_';
    }

    /**
     * Processes one file with state machine
     * @param fileName Source Code file name
     */
    private void processFile(String fileName){
        try {
            File file = new File(new File(fileName).getAbsolutePath());
            currFile = file.getName();
            logger.info(
                    MessageFormat.format("Processing: {0}", currFile));
            BufferedReader reader =
                    new BufferedReader(new FileReader(file));
            int numRead;
            State state = State.CODE;
            ExtraState extraState = ExtraState.SKIP;            
            StringBuilder buffer = new StringBuilder();
            StringBuilder extraBuffer = new StringBuilder();            
            StringBuilder extra2Buffer = new StringBuilder();
            String comment=null;
            char ch;
            while((numRead=reader.read())!=-1){
                ch =(char)numRead;
                buffer.append(ch);
                switch(state){
                    case CODE:
                        switch (extraState){
                            case SKIP:
                                break;
                            case SPACE:
                                if (isWhite(ch)){
                                    break;
                                }
                                extraState = ExtraState.READ;
                                /* fall through */
                            case READ:
                                if (isWhite(ch)){
                                    extraState = ExtraState.SPACE2;
                                    break;
                                }
                                extraBuffer.append(ch);
                                break;
                            case SPACE2:
                                if (isWhite(ch)){
                                    break;
                                }
                                extraState = ExtraState.READ2;
                                /* fall through */
                            case READ2:
                                if (isWhite(ch)){
                                    extraState = ExtraState.SKIP;
                                    break;
                                }
                                extra2Buffer.append(ch);
                                break;
                        }                                            
                        if (StringUtils.endsWith(buffer, START_COMMENT)){
                            if (comment!=null){
                                // comment is null before the first comment starts
                                // so we do not process it
                                processComment(comment,
                                        extraBuffer.toString(), extra2Buffer.toString());
                            }
                            extraBuffer.setLength(0);
                            extra2Buffer.setLength(0);
                            buffer.setLength(0);
                            state = State.COMMENT;
                        }
                        break;
                    case COMMENT:
                       if (StringUtils.endsWith(buffer, END_COMMENT)){
                            comment =
                                    buffer.substring(0,
                                            buffer.length()-END_COMMENT.length());
                            buffer.setLength(0);
                            state = State.CODE;
                            extraState = ExtraState.SPACE;
                        }
                        break;
                }
            }
            processComment(comment,
                    extraBuffer.toString(), extra2Buffer.toString());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createClassHierarchy(){
        for(DocClass docClass: classes){
            for(DocClass cls: classes){
                if(docClass.className.equals(cls.parentClass)){
                    ClassDescr subClass = new ClassDescr();
                    subClass.className = cls.className;
                    subClass.shortClassName = cls.shortClassName;
                    docClass.subClasses.add(subClass);
                    cls.parent = docClass;
                }
            }
            for(DocCfg cfg: cfgs){
                if(docClass.className.equals(cfg.className)){
                    docClass.cfgs.add(cfg);
                }
            }
            for(DocProperty property: properties){
                if(docClass.className.equals(property.className)){
                    docClass.properties.add(property);
                }
            }
            for(DocMethod method: methods){
                if(docClass.className.equals(method.className)){
                    docClass.methods.add(method);
                }
            }
            for(DocEvent event: events){
                if(docClass.className.equals(event.className)){
                    docClass.events.add(event);
                }
            }
        }
    }

    private <T extends DocAttribute> boolean isOverridden(T doc,
                                                          List<T> docs){
        if (doc.name == null || doc.name.isEmpty()) return false;
        for(DocAttribute attr:docs){
            String docName = StringUtils.separateByLastDot(doc.name)[1];
            String attrName = StringUtils.separateByLastDot(attr.name)[1];
            if (docName.equals(attrName)) return true;
        }
        return false;
    }

    private <T extends DocAttribute> void removeHidden
                                                                                        (List<T> docs){
        for(ListIterator<T> it = docs.listIterator(); it.hasNext();){
            if (it.next().hide)
                it.remove();
        }
    }

    private <T extends DocAttribute> void addInherited
                                                    (List<T> childDocs, List<T> parentDocs){
        for(T attr: parentDocs) {
            if (!isOverridden(attr, childDocs) && !attr.isStatic){
                childDocs.add(attr);
            }
        }
    }


    private void injectInherited(){
        for(DocClass cls: classes){
            DocClass parent = cls.parent;
            while(parent!=null){
                ClassDescr superClass = new ClassDescr();
                superClass.className = parent.className;
                superClass.shortClassName = parent.shortClassName;
                cls.superClasses.add(superClass);
                if (parent.className.equals(COMPONENT_NAME)){
                    cls.component = true;
                }
                addInherited(cls.cfgs, parent.cfgs);
                addInherited(cls.properties, parent.properties);
                addInherited(cls.methods, parent.methods);
                addInherited(cls.events, parent.events);
                parent = parent.parent;
            }
            removeHidden(cls.cfgs);
            removeHidden(cls.properties);
            removeHidden(cls.methods);
            removeHidden(cls.events);

            // sorting
            Collections.sort(cls.cfgs);
            Collections.sort(cls.properties);
            Collections.sort(cls.methods);
            Collections.sort(cls.events);

            Collections.reverse(cls.superClasses);
            Collections.sort(cls.subClasses);

        }
    }

    private void createPackageHierarchy(){
        for(DocClass cls: classes){
            tree.addClass(cls);
        }
        tree.sort();
    }

    private void showStatistics(){
        logger.info("*** STATISTICS ***") ;
        for (Map.Entry<String, Integer> e : Comment.allTags.entrySet()){
            logger.info(e.getKey() + ": " + e.getValue());        
        }
    }

    public void process(String fileName){
        try {
            File xmlFile = new File(new File(fileName).getAbsolutePath());
            FileInputStream fileInputStream = new FileInputStream(xmlFile);
            JAXBContext jaxbContext =
                    JAXBContext.newInstance("extdoc.jsdoc.schema");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            extdoc.jsdoc.schema.Doc doc =
                    (extdoc.jsdoc.schema.Doc) unmarshaller.
                            unmarshal(fileInputStream);
            extdoc.jsdoc.schema.Source source = doc.getSource();
            List<extdoc.jsdoc.schema.File> files = source.getFile();
            for(extdoc.jsdoc.schema.File file: files){
                processFile(xmlFile.getParent()+ File.separator +file.getSrc());
            }
            showStatistics();
            fileInputStream.close();
            createClassHierarchy();
            injectInherited();
            createPackageHierarchy();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


     private void copyDirectory(File sourceLocation , File targetLocation)
        throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (String child : children) {
                copyDirectory(new File(sourceLocation, child),
                        new File(targetLocation, child));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public void saveToFolder(String folderName, String templateFileName){
        new File(folderName).mkdirs();
        try {

            File templateFile =
                    new File(new File(templateFileName).getAbsolutePath());
            String templateFolder = templateFile.getParent();

            // Read template.xml
            JAXBContext jaxbTplContext =
                    JAXBContext.newInstance("extdoc.jsdoc.tplschema");
            Unmarshaller unmarshaller = jaxbTplContext.createUnmarshaller();
            Template template = (Template) unmarshaller.
                        unmarshal(new FileInputStream(templateFile));
            ClassTemplate classTemplate = template.getClassTemplate();
            String classTplFileName = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(classTemplate.getTpl())
                    .toString();
            String classTplTargetDir = new StringBuilder()
                    .append(folderName)
                    .append(File.separator)
                    .append(classTemplate.getTargetDir())
                    .toString();
            TreeTemplate treeTemplate = template.getTreeTemplate();
            String treeTplFileName = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(treeTemplate.getTpl())
                    .toString();
            String treeTplTargetFile = new StringBuilder()
                    .append(folderName)
                    .append(File.separator)
                    .append(treeTemplate.getTargetFile())
                    .toString();

            logger.info("*** COPY RESOURCES ***") ;
            new File(classTplTargetDir).mkdirs();

            // Copy resources
            Resources resources = template.getResources();

            List<Copy> dirs = resources.getCopy();

            for(Copy dir : dirs){
                String src = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(dir.getSrc())
                    .toString();
                String dst = new StringBuilder()
                    .append(folderName)
                    .append(File.separator)
                    .append(dir.getDst())
                    .toString();
                copyDirectory(new File(src), new File(dst));
            }

            // Marshall and transform classes
            JAXBContext jaxbContext =
                    JAXBContext.newInstance("extdoc.jsdoc.docs");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT,
                    true
            );
            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);

            TransformerFactory factory = TransformerFactory.newInstance();
            Templates transformation = 
                    factory
                            .newTemplates (new StreamSource(classTplFileName)) ;
            Transformer transformer = transformation.newTransformer();

            DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();

            logger.info("*** SAVING FILES ***") ;
            for(DocClass docClass: classes){
                logger.info("Saving: " + docClass.className);
                String targetFileName = new StringBuilder()
                        .append(classTplTargetDir)
                        .append(File.separator)
                        .append(docClass.className)
                        .append('.')
                        .append(OUT_FILE_EXTENSION)
                        .toString();
                Document doc = docBuilder.newDocument();
                marshaller.marshal(docClass, doc);
                if (GENERATE_DEBUG_XML){
                    marshaller.marshal(docClass, new File(targetFileName+"_"));
                }
                Result fileResult = new StreamResult(new File(targetFileName));
                transformer.transform(new DOMSource(doc), fileResult);
                transformer.reset();
            }

            // Marshall and transform tree
            JAXBContext jaxbTreeContext =
                    JAXBContext.newInstance("extdoc.jsdoc.tree");
            Marshaller treeMarshaller = jaxbTreeContext.createMarshaller();
            treeMarshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT,
                    true
            );

            Templates treeTransformation =
                    factory
                            .newTemplates (new StreamSource(treeTplFileName)) ;
            Transformer treeTransformer =
                    treeTransformation.newTransformer();
            Document doc =
                        builderFactory.newDocumentBuilder().newDocument();
            treeMarshaller.marshal(tree, doc);
            if (GENERATE_DEBUG_XML){
                    treeMarshaller.
                            marshal(tree, new File(treeTplTargetFile+"_"));
            }
            Result fileResult = new StreamResult(new File(treeTplTargetFile));
            treeTransformer.transform(new DOMSource(doc), fileResult);

        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
