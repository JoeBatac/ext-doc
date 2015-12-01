# Introduction #

While Ext-Doc comes out-of-the-box with a number of helpful tags, it's often desirable to create custom tags which document unique functionality within your application.

## Step One ##

In your XML file (which defines which JS files are parsed for documentation), you should see a section marked TAGS:

```
<doc>
  <sources>
  </sources>

  <tags>
  </tags>
</doc>
```

The TAGS section allows custom tags to be added for every "documantable item" i.e. class, cfg, property, event. The custom tag list is accessible in XSLT-template and the following properties:
  * name
  * title
  * format

Example:
```
<doc>
  <sources>
  </sources>

  <tags>
    <tag name="default" title="Default Value" format="&lt;i&gt;{0}&lt;/i&gt;" />
  </tags>
</doc>
```

The resulting HTML of the above custom tag would be:
```
<i>Default Value</i>: The value of our @default tag
```

## Step Two ##

The next step is to add your custom tag to your JavaScript documentation comments. The "name" attribute you defined in the XML file is now the name of your new tag. In the case of our example, the custom "default" tag would be used as @default:

```
/**
 * @property
 * @type string
 * @default 'This is our default string value.'
 */
this.myProperty = 'This is our default string value.';
```