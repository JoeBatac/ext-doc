# Introduction #

Several tags are best defined at the class-level as they describe the class in specific detail. These include:

  * @class
  * @namespace
  * @extends
  * @singleton
  * @cfg
  * @constructor and @param


## 1.0 @class ##

The @class tag is the most basic and important tag in Ext-Doc.

It tells the Ext-Doc documentation generator:
  * to list the class in the namespace tree, and
  * to create an output page for the class

The @class tag displays any text after the class name as a description of that class.

Example:
```
/**
 * @class MyClassName A description of MyClassName
 * /
```

## 1.1 @namespace ##

The @namespace tag defines a class within a parent namespace.

It tells the Ext-Doc documentation generator:
  * to list the class in the documentation tree underneath the specified namespace node.

The @namespace tag is not required and does not allow for a description.

Example:
```
/**
 * @class SamplePackage.MyClassName A description of MyClassName
 * @namespace SamplePackage
 * /
```

## 1.2 @extends ##

The @extends tag defines a class as inheriting data and/or functionality from a parent class.

It tells the Ext-Doc documentation generator:
  * to place a link to the parent class (if possible) in the header of the class documentation page.

The @extends tag is not required and does not allow for a description.

Example:
```
/**
 * @class MyClassName A description of MyClassName
 * @extends ParentClass
 * /
```

## 1.3 @singleton ##

The @singleton tag defines a class as static (i.e. there can be no new instances of this class).

It tells the Ext-Doc documentation generator:
  * to use a "singleton" icon in the documentation tree
  * to place the text "This class is a singleton and cannot be created directly." in the documentation page for the class

The @singleton tag is not required and does not allow for a description.

Example:
```
/**
 * @class MyClassName A description of MyClassName
 * @singleton
 * /
```

## 1.4 @cfg ##

The @cfg tag defines possible configuration parameters for the class. More than one @cfg tag may be defined for any class.

It tells the Ext-Doc documentation generator:
  * to create a "Config Options" section at the top of the class documentation page

The @config tag is not required, but is formatted with:
  * the type of the parameter (noted between a pair of curly braces)
  * the name of the parameter
  * a description of the parameter

Example:
```
/**
 * @class MyClassName A description of MyClassName
 * @cfg {type} optionA A description of the configuration option
 * @cfg {type} optionB A description of the configuration option
 * /
```

## 1.5 @constructor ##

The @constructor tag defines a constructor for the class. It should be followed by any necessary @param tags which describe any parameters passed into the constructor. Typically, one of the parameters passed into the constructor will be an object which assigns values to any required attributes described by the @cfg tags.

It tells the Ext-Doc documentation generator:
  * to place the documentation for the constructor as the first method listed in the "Public Methods" section of the class documentation page.

The @constructor tag is not required and does not allow for a description.

Any @param tags which are defined are formatted as:
  * the type of the parameter (noted between a pair of curly braces)
  * the name of the parameter
  * a description of the parameter

Example:
```
/**
 * @class MyClassName A description of MyClassName
 * @constructor
 * @param {type} myParameter A description of the parameter
 * /
```

## 2.0 Putting things together ##

Assuming your class needs more than one of the above tags, you might document your class as follows:

```
/**
 * @class Car.Radio A simple car radio
 * @namespace Car
 * @extends ElectronicThing
 * @cfg {integer} numberOfButtons The number of buttons on this radio.
 * @cfg {boolean} hasCdPlayer True if the radio has a CD player; otherwise false.
 * @constructor
 * @param {object} configObj 
 * An object containing the required configuration options for this class
 * /
Car.Radio = function(configObj) {
  ...
};
```