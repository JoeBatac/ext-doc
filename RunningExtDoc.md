# Introduction #

For those of us who aren't great at Java, it is helpful to have the setup process explained in plain English.

Perhaps the easiest way to run Ext-Doc is to create a batch file of some kind so that you don't have to manually write Java into the command line. For Windows users, this is definitely the best way to do it.

# First things first #

Download the Ext-Doc zip file and extract its contents to your machine. Inside the folder, you'll see the following:
  * /lib/
  * /output/
  * /sample/
  * /template/
  * ext-doc.jar
  * README.txt

The README file does a nice job explaining what you need to do to get started. This Wiki page is simply going to expand on the idea for real-world usage (i.e. how I did it on my own machine).

Using this Wiki for reference, start documenting your code. Each JS file that you want included in your documentation **must** be mentioned in an XML file - a default example of this file can be found in /sample/ext.xml

I suggest copying this file to the same folder where you save your source code. Rename the file _**documentation.xml**. Instructions on how to add files to the XML are located within that file itself._

For the sake of example, let's say our code is in **C:/MyCode/**

# On Windows #

If you're running Windows Vista (or any version with IIS installed) then you're in luck! In this case, we can use the local IIS server to run Ext-Doc rather than uploading our code to a remote server. Obviously, you'll need to setup IIS - this tutorial assumes you've already done that.

Open a simple text editor like Notepad. We will write a handful of lines of basic code to run the Java commands for us on demand.

Notice where you have extracted the Ext-Doc source files. On my machine, it's at **C:/MyDocuments/Ext-Doc/**

In our text editor (e.g. Notepad), copy the following code. **Be sure to change any file paths to match your system!**

```
set extDocJarFile="\MyDocuments\Ext-Doc\ext-doc.jar"

set extDocXMLFile="\MyCode\_documentation.xml"

set extDocTemplate="\MyDocuments\Ext-Doc\template\ext\template.xml"

set outputFolder="C:/inetpub/wwwroot"

java -jar %extDocJarFile% -p %extDocXMLFile% -o %outputFolder% -t %extDocTemplate%  -verbose
```

Save this file as **generateDocs.bat**.

## Running Ext-Doc ##

Now that we have our .bat file, we can simply double-click and run it as any executable file. A Windows command prompt window will open and close relatively quickly as code is executed. _This may prevent you from seeing errors which are output to the command prompt._ If things aren't working correctly, simply paste all the code from our .bat file into a new Windows command prompt window.

Assuming you don't have any errors, you can simply open a web browser and go to your local host (probably http://localhost/).