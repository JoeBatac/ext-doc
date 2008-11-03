package extdoc.jsdoc.processor;

import extdoc.jsdoc.docs.*;
import extdoc.jsdoc.tags.*;
import extdoc.jsdoc.tags.impl.Comment;
import extdoc.jsdoc.tplschema.*;
import extdoc.jsdoc.tree.TreePackage;
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
import java.util.ArrayList;
import java.util.List;


/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 4:41:12
 */
public class FileProcessor{

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

    private String className;
    private String shortClassName;
    private String currFile;

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
            param.description = paramTag.getParamDescription();
            param.optional = paramTag.isOptional();
            params.add(param);
        }
    }

    private String[] dividePackage(String className){
        String[] str = new String[2];
        int len = className.length();
        int i = len-1;
        while(i>=0 && className.charAt(i)!='.') i--;
        str[0] = (i>0)?className.substring(0,i):"";
        str[1] = className.substring(i+1,len);
        return str;
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
        String[] str = dividePackage(cls.className);
        cls.packageName = str[0];
        cls.shortClassName = str[1];
        cls.definedIn = currFile;
        cls.singleton = singletonTag!=null;
        String description = classTag.getClassDescription();
        if (description==null && extendsTag!=null){
            description = extendsTag.getClassDescription();
        }
        cls.description = description;
        cls.parentClass =
                (extendsTag!=null)?extendsTag.getClassName():null;
        cls.hasConstructor = constructorTag!=null;
        if (constructorTag!=null){
            cls.constructorDescription = constructorTag.text();
            readParams(paramTags, cls.params);
        }
        classes.add(cls);
        className = cls.className;
        shortClassName = cls.shortClassName;
    }

    /**
     * Process cfg
     * @param comment Comment
     */
    private void processCfg(Comment comment){
        DocCfg cfg = new DocCfg();
        CfgTag tag = comment.tag("@cfg");
        cfg.name = tag.getCfgName();
        cfg.type = tag.getCfgType();
        cfg.description = tag.getCfgDescription();
        cfg.optional = tag.isOptional();
        cfg.className = className;
        cfg.shortClassName = shortClassName;
        cfgs.add(cfg);
    }

    /**
     * Process property 
     * @param comment Comment
     * @param extraLine first word form the line after comment
     */
    private void processProperty(Comment comment,String extraLine){
        DocProperty property = new DocProperty();

        Tag propertyTag = comment.tag("@property");
        TypeTag typeTag = comment.tag("@type");

        property.name = extraLine;
        if (propertyTag!=null
                && propertyTag.text()!=null 
                && propertyTag.text().length()>0){
            property.name = propertyTag.text();
        }
        property.type = typeTag.getType();
        property.description = comment.getDescription();
        property.className = className;
        property.shortClassName = shortClassName;
        properties.add(property);
    }

    /**
     * Process method 
     * @param comment Comment
     * @param extraLine first word form the line after comment
     */
    private void processMethod(Comment comment, String extraLine){
        DocMethod method = new DocMethod();

        Tag methodTag = comment.tag("@method");
        Tag staticTag = comment.tag("@static");
        List<ParamTag> paramTags = comment.tags("@param");
        ReturnTag returnTag = comment.tag("@return");
        MemberTag memberTag = comment.tag("@member");

        // should be first because @member may redefine class
        method.className = className;
        method.shortClassName = shortClassName;
        method.name = extraLine;
        if (methodTag!=null){
            method.name = methodTag.text();
        }
        if (memberTag!=null){
            method.name = memberTag.getMethodName();
            method.className = memberTag.getClassName();
        }
        method.isStatic = (staticTag!=null);
        method.description = comment.getDescription();
        if (returnTag!=null){
            method.returnType =returnTag.getReturnType();
            method.returnDescription =returnTag.getReturnDescription();
        }
        readParams(paramTags, method.params);
        methods.add(method);
    }

    /**
     * Process event
     * @param comment Comment
     */
    private void processEvent(Comment comment){
        DocEvent event = new DocEvent();
        EventTag eventTag = comment.tag("@event");
        List<ParamTag> paramTags = comment.tags("@param");
        event.name = eventTag.getEventName();
        event.description = eventTag.getEventDescription();
        readParams(paramTags, event.params);
        event.className = className;
        event.shortClassName = shortClassName;
        events.add(event);
    }

    /**
     *  Determine type of comment and process it
     * @param content text inside / ** and * /
     * @param extraLine first word form the line after comment 
     */
    private void processComment(String content, String extraLine){
        if (content==null) return;
        Comment comment = new Comment(content);
        if(comment.hasTag("@class")){
            processClass(comment);
        }else if(comment.hasTag("@event")){
            processEvent(comment);
        }else if(comment.hasTag("@cfg")){
            processCfg(comment);
        }else if(comment.hasTag("@type")){
            processProperty(comment, extraLine);        
        }else{
            processMethod(comment, extraLine);            
        }
        
    }

    private enum State {CODE, COMMENT}
    private enum ExtraState {SKIP, SPACE, READ}   

    private static final String START_COMMENT = "/**";
    private static final String END_COMMENT = "*/";

    /**
     * Checks if StringBuilder ends with string
     */
    private boolean endsWith(StringBuilder sb, String str){
        int len = sb.length();
        int strLen = str.length();        
        return (len>=strLen && sb.substring(len-strLen).equals(str));
    }

    /**
     * Checks if char is white space in terms of extra line of code after
     * comments
     * @param ch character
     * @return true if space or new line or * or / or ' etc...
     */
    private boolean isWhite(char ch){
        return !Character.isLetterOrDigit(ch);
    }

    /**
     * Processes one file with state machine
     * @param fileName Source Code file name
     */
    private void processFile(String fileName){
        try {
            File file = new File(new File(fileName).getAbsolutePath());
            currFile = file.getName();
            System.out.println(
                    MessageFormat.format("Processing: {0}", currFile));
            BufferedReader reader =
                    new BufferedReader(new FileReader(file));
            int numRead;
            State state = State.CODE;
            ExtraState extraState = ExtraState.SKIP;            
            StringBuilder buffer = new StringBuilder();
            StringBuilder extraBuffer = new StringBuilder();            
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
                                    extraState = ExtraState.SKIP;
                                    break;
                                }
                                extraBuffer.append(ch);
                                break;
                        }                                            
                        if (endsWith(buffer, START_COMMENT)){
                            if (comment!=null){
                                // comment is null before the first comment starts
                                // so we do not process it
                                processComment(comment, extraBuffer.toString());
                            }
                            extraBuffer.setLength(0);
                            buffer.setLength(0);
                            state = State.COMMENT;
                        }
                        break;
                    case COMMENT:
                       if (endsWith(buffer, END_COMMENT)){
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
            processComment(comment, extraBuffer.toString());
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
        if (doc.name == null) return false;
        for(DocAttribute attr:docs){
            if (doc.name.equals(attr.name)) return true;
        }
        return false;
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
                for(DocCfg cfg: parent.cfgs) {                    
                    if (!isOverridden(cfg, cls.cfgs)){
                        cls.cfgs.add(cfg);
                    }
                }
                for(DocProperty property: parent.properties){
                    if (!isOverridden(property, cls.properties)){
                        cls.properties.add(property);
                    }
                }
                for(DocMethod method: parent.methods){
                    if (!isOverridden(method, cls.methods)){
                        cls.methods.add(method);
                    }
                }
                for(DocEvent event: parent.events){
                    if (!isOverridden(event, cls.events)){
                        cls.events.add(event);
                    }
                }
                parent = parent.parent;
            }
        }
    }

    private void createPackageHierarchy(){
        for(DocClass cls: classes){            
            tree.addClass(cls);
        }
    }

    public void process(String fileName){
        try {
            File xmlFile = new File(fileName);
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

            for(DocClass docClass: classes){
                System.out.println("Saving: " + docClass.className);
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