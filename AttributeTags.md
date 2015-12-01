# Introduction #

Several tags are best described at the attribute-level as they describe class properties in great detail. These include:

  * @cfg
  * @property and @type
  * @method, @param and @return
  * @event

## 1.0 @cfg ##

The @cfg tag defines possible configuration parameters for the class. In most cases, the @cfg tag is used at the [Class-Level](ClassTags.md) as a stand-alone tag; however, it is available at the attribute-level for convenience.

It tells the Ext-Doc documentation generator:
  * to create a "Config Options" section at the top of the class documentation page

The @config tag is not required, but is formatted with:
  * the type of the parameter (noted between a pair of curly braces)
  * the name of the parameter
  * a description of the parameter

Example:
```
MyClass = function(configObj) {
  /**
   * @cfg {string} name The name of our MyClass instance
   */
  this.name = configObj.name;
};
```

## 2.0 @property and @type ##

The @property tag defines a non-functional class attribute.

It tells the Ext-Doc documentation generator:
  * to list the attribute in the "Public Properties" section of the class documentation page

The @property tag is not required and does not allow for a description. However, it is highly encouraged to use the @type tag when defining a property. The @type tag takes a single word (e.g. 'string', 'integer', etc.) as the description for a property's type.

Example:
```
MyClass = function() {
  /**
   * @property
   * @type string
   */
  this.name = 'Steve';
};
```

**NOTE:** Properties may be declared without the @property tag (using just the @type or any custom tags), as Ext-Doc will parse the code after doc-declaration to check if this is a method or property. However, it is encouraged to use the @property tag for clarity within your own code.

## 3.0 @method, @param and @return ##

The @method tag defines a functional class attribute.

It tells the Ext-Doc documentation generator:
  * to list the method in the "Public Methods" section of the class documentation page

The @method tag is not required and does not allow for a description. However, it is encourages that you use the @param and @return tags when defining a method.

The @param tag is formatted as:
  * the type of the paramter (contained within a set of curly braces)
  * the parameter's name
  * a description of the parameter

The @return tag is formatted as:
  * the name of the return parameter
  * a description of the returned parameter

Example:
```
MyClass = function() {
  /**
   * @method
   * @param {type} someParameter A description of the parameter
   * @return returnValue A description of the value returned by this method
   */
  this.refresh = function(someParameter) {
    //some functional logic
    return returnValue;
  };
};
```

**NOTE:** Methods may be declared without the @method tag (using just the @param, @return or any custom tags), as Ext-Doc will parse the code after doc-declaration to check if this is a method or property. However, it is encouraged to use the @method tag for clarity within your own code.

## 4.0 @event ##

The @event tag defines some observable event managed by the class.

It tells the Ext-Doc documentation generator:
  * to list the event in the "Public Events" section of the class documentation page

The @event tag is not required. It is formatted as:
  * the event's name
  * a description of the event

The @event tag is usually followed by @param tags describing any parameters passed to the event's handler method.

The @param tag is formatted as:
  * the type of the paramter (contained within a set of curly braces)
  * the parameter's name
  * a description of the parameter

Example:
```
MyClass = {
  initComponent : function(){
    Ext.Button.superclass.initComponent.call(this);

    this.addEvents(
      /**
       * @event click
       * Fires when this button is clicked
       * @param {Button} this
       * @param {EventObject} e The click event
       */
      "click"
    );
  }
};
```