# Galleon Feature-Pack for Integrating a Jakarta EE 8 to EE 9 Deployment Transformation Capability into a WildFly Installation.

This feature-pack provides a JBoss Modules module that allows WildFly to analyze the bytecode and other resources of any 
managed deployment looking for use of Jakarta EE 8 APIs. If found, the copy of the deployment content stored in the server's internal
deployment content repository is bytecode transformed to instead use the Jakarta EE 9 APIs. Jakarta EE 8 and EE 9 have equivalent APIs,
except for the change of all package names from javax.* to jakarta.*. 

The transformation happens once, when the content is stored in the content repository. 
The original content that was provided to the server is not changed. This includes war, ear or jar files placed in the WildFly `deployments` directory.

"Unmanaged" deployments (for example an exploded deployment in the `deployments` directory) are not transformed. The transformation happens when the
server makes a copy of the deployment for internal use, and no such copy is made for unmanaged deployments.

This functionality has been part of WildFly Preview since its first release. This feature pack allows the same functionality to be added to standard WildFly.

---
**NOTE About Jakarta EE 10**

This deployment transformation does not detect or attempt to fix any EE API usage that is no longer supported or behaves differently
in Jakarta EE 10. WildFly 27 and later support EE 10. EE 10 differs from EE 8 in more than just the javax to jakarta package rename. It includes
a number of other API changes, largely consisting of the removal of long-deprecated API. Applications that rely on APIs that were removed in EE 10
will need to have their source code migrated.

---

There is no configuration (e.g. settings in standalone.xml) associated with this capability. If the module provisioned by this feature-pack is on the module path, the capability will be used.

The deployment-transformer Galleon feature-pack is to be provisioned along with the WildFly Galleon feature-pack.

Resources:

* [WildFly Installation Guide](https://docs.wildfly.org/27/#installation-guides)
* [Galleon documentation](https://docs.wildfly.org/galleon/)


## Galleon Feature-Pack Compatible with Standard WildFly

The Maven coordinate to use is: `org.wildfly:wildfly-deployment-transformer-feature-pack`

## No Galleon Feature-Pack Compatible with WildFly Preview

WildFly Preview includes EE 8 to EE 9 deployment transformation functionality out of the box; therefore this project does not produce a feature pack for use with WildFly Preview. 
If this out of the box functionality is ever removed from WildFly Preview, an additional feature pack for WildFly Preview will be added here.

# Using the Deployment Transformer Galleon Feature-Pack

Provisioning of the deployment-transformer capability can be done in multiple ways according to the provisioning tooling in use.

## Provisioning Using the Galleon CLI Tool

You can download the latest Galleon CLI tool from the Galleon github project [releases](https://github.com/wildfly/galleon/releases).
 
You need to define a Galleon provisioning configuration file such as:

```
<?xml version="1.0" ?>
<installation xmlns="urn:jboss:galleon:provisioning:3.0">
  <feature-pack location="org.wildfly:wildfly-galleon-pack:27.0.1.Final">
    <default-configs inherit="false"/>
    <packages inherit="false"/>
  </feature-pack>
  <feature-pack location="org.wildfly:wildfly-wildfly-deployment-transformer-feature-pack:1.0.0.Alpha1">
    <default-configs inherit="false"/>
    <packages inherit="false"/>
  </feature-pack>
  <config model="standalone" name="standalone.xml">
    <layers>
      <include name="jaxrs-server"/>
    </layers>
  </config>
  <options>
    <option name="optional-packages" value="passive+"/>
    <option name="jboss-fork-embedded" value="true"/>
  </options>
</installation>
```
and provision it using the following command:

```
galleon.sh provision provisioning.xml --dir=my-wildfly-server
```

If you wish to provision everything included in a standard WildFly distribution, your provisioning.xml can be quite a bit simpler:

````
<?xml version="1.0" ?>
<installation xmlns="urn:jboss:galleon:provisioning:3.0">
  <feature-pack location="org.wildfly:wildfly-galleon-pack:27.0.1.Final"/>
  <feature-pack location="org.wildfly:wildfly-wildfly-deployment-transformer-feature-pack:1.0.0.Alpha1"/>
</installation>
````


## Provisioning using Maven

The WildFly project provides Maven plugins that allow you to provision traditional server installations or bootable jars as part of your Maven build:

* For a traditional server installation, use the the [WildFly Maven Plugin](https://docs.wildfly.org/wildfly-maven-plugin/).
* For a bootable jar, use the [WildFly JAR Maven plugin](https://docs.wildfly.org/bootablejar/).

The details of how to configure these plugins is beyond the scope of this document; see the plugin documentation linked above for further details. However, both have similar configuration blocks for their mojos used for provisioning. In both, 
you need to include the deployment-transformer feature-pack in the Maven Plugin configuration.along with the feature-pack declaration for the standard WildFly feature-pack.

This looks like:

```
...
<feature-packs>
  <feature-pack>
    <location>org.wildfly:wildfly-galleon-pack:27.0.1.Final</location>
  </feature-pack>
  <feature-pack>
    <location>org.wildfly:wildfly-wildfly-deployment-transformer-feature-pack:1.0.0.Alpha1</location>
  </feature-pack>
</feature-packs>
<layers>
  <layer>jaxrs-server</layer>
</layers>
...
```

The `layers` element in the example above is optional. With the WildFly Maven Plugin, if the `layers` element is not present the server installation will include all content, analogous to what's in the zip available from https://wildfly.org/downloads. With the WildFly Jar Maven Plugin, if the `layers` element is not present the bootable jar will include a configuration file analogous to what is in the traditional server distribution's `standalone-microprofile.xml` file, along with the necessary modules needed to support that configuration.

