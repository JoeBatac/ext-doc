package extdoc.jsdoc.docs;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 5:16:41
 */

@XmlRootElement
public class DocClass{
    public String className;
    public String shortClassName;
    public String packageName;
    public String definedIn;
    public boolean singleton;
    public String description;
    public String parentClass;
    public boolean hasConstructor;
    public String constructorDescription;
    public List<Param> params = new ArrayList<Param>();
    public List<DocCfg> cfgs = new ArrayList<DocCfg>();
    public List<DocProperty> properties = new ArrayList<DocProperty>();
    public List<DocMethod> methods = new ArrayList<DocMethod>();
    public List<DocEvent> events = new ArrayList<DocEvent>();
    public List<DocClass> subClasses = new ArrayList<DocClass>();
    @XmlTransient
    public DocClass parent = null;
}