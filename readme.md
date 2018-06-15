
 '''
 esempio di configurazione
    <plugin>
            <groupId>factory.framework</groupId>
            <artifactId>framework-thrift2Java-maven-plugin</artifactId>
            <version>2.5.0-SNAPSHOT</version>
            <configuration>
                <thriftExecutable>C:/Program Files (x86)/Apache/thrift/bin/thrift</thriftExecutable>
                <thriftSourceRoot>${project.basedir}/src/main/thrift</thriftSourceRoot>
            </configuration>
            <executions>
                <execution>
                    <!--<phase>non serve, tutti goal vengono già eseguiti in compile phase</phase>-->
                    <goals>
                        <goal>genjava</goal> <!-- genera il java dai file thrift -->
                        <goal>Java2CxfWs</goal><!-- converte i servizi java in wsdl -->
                    </goals>
                </execution>
            </executions>
        </plugin>
'''

*** DA AGGIORNARE***


'''
Plugin sequence operations:

1-	dal file trift generiamo le classi java

i thrift devono essere letti dalla src\main\thrift del progetto

2-annotandole per renderle adeguate a CXF e WAX secondo queste specifiche:


## NB:
*  i file thrift conterranno il nome del package dichiarando al loro interno

"namespace java drift.<progetto>.<modulo>.<api|service>.<servizio>”

quindi alla generazione
namespace java drift.<progetto>.<modulo>.<api|service>.<servizio>”  produrranno -->

src/main/java/drift/progetto/modulo/api/servizio
o
src/main/java/drift/progetto/modulo/service/servizio

il plugin quindi deve solo lanciare il comando thrift con le opzioni come espresso manaulamente più sotto (vedi FASI MANUALI)
ma deve tenere conto di rielaborare le classi aggiungendo o modificando seguendo queste specifiche:

* Le liste devono essere annotate tipo Java Factory ma sul field e non sul getter

* nel jar deve essere presente nella META-INF:
                  - file drift-services.list contenente l'elenco dei servizi disponibili (elenco wsdl)
                  - cartella wsdl contenente i wsdl dei servizi nome tipo: drift-<modulo>@<servizio>.wsdl (vedi Java Factory)

* per tutte le classi generate, aggiungere a livello di classe l'annotazione:

               @XmlAccessorType(XmlAccessType.FIELD)

* più relativi import:
               import javax.xml.bind.annotation.XmlAccessType;
               import javax.xml.bind.annotation.XmlAccessorType;


3-	compiliamo mediante maven le classi e produciamo il jar
4-	mediante CXF generiamo i wsdl e con questo possiamo interfacciare servizi scritti in altri linguaggi su altri sistemi

CXF ha un suo plugin javaws (vedere come integrarlo nel nostro plugin)
java2ws -cp
"./target/drift-thrift-api-1.0.1-SNAPSHOT.jar;C:\Progetti\.m2\repository\org\apache\thrift\libthrift\0.11.0\libthrift-0.11.0.jar"
-verbose -wsdl
-o calculator.wsdl
drift.drift.thrift.api.tutorial.Calculator$Iface
(i wsdl devono essere spostati nella cartella META-INF del jar e avere nomenclatura nel nome file : drift-<modulo>@<servizio>.wsdl
'''


Il plugin deve avere secondo me le seguenti specifiche:

## Plugin phase: deve essere eseguito durante  tutto il default build lifecycle ( dovrebbe quindi inglobare già compiersi dalla "generate-sources"	(generate any source code for inclusion in compilation)).
## NB: non dare fase la plugin, la fase di limite va solo specificata nel progetto che utilizzerà il plugin nel tag execute)
##  plugin goal , si può dare un nome tipo:  java (nome inventato utile per lanciare il plugin da linea di comando)
il nome del plugin è thrift2Java
'''
provare ad utilizzare l'annotazione sull'execute:@Mojo( name = "genjava")

Per includere poi il nostro plugin nel progetto wax:

<build>
....
<plugins>
      <plugin>
        <groupId>com.cadit</groupId>
        <artifactId>nomeArtefatto-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
      </plugin>
    </plugins>
..
...
</build>

Il nome Artefatto è l'artifactId del nostro plugin cioè "framework-thrift2Java"

Per generare il plugin sono partito da

mvn archetype:generate \
  -DgroupId=factory.framework \
  -DartifactId=framework-thrift2Java-maven-plugin \
  -DarchetypeGroupId=org.apache.maven.archetypes \
  -DarchetypeArtifactId=maven-archetype-plugin

chiederà la versione, mettere la 2.5.0-SNAPSHOT

e dal già pronto framework-rest-maven-plugin
'''

#### FASI MANUALI:

'''
## 1. generazione JAVA:
aprire un cmd nella src\main\thrift ed eseguire:
               thrift -r --gen java -v -debug -out ../java tutorial.thrift

## 2. modifiche ai file pre generazione cxf
per tutte le classi generate: aggiungere a livello di classe l'annotazione:

               @XmlAccessorType(XmlAccessType.FIELD)

e relativi import:
               import javax.xml.bind.annotation.XmlAccessType;
               import javax.xml.bind.annotation.XmlAccessorType;

3. ## compilare le classi (mvn clean install del progetto)

cxf:
da dentro la cartella del progetto:

"C:\Program Files (x86)\Apache\cxf\apache-cxf-3.2.2\bin\java2ws" -cp "./target/drift-thrift-api-1.0.1-SNAPSHOT.jar;C:\Progetti\.m2\repository\org\apache\thrift\libthrift\0.11.0\libthrift-0.11.0.jar" -verbose -wsdl -o calculator.wsdl drift.drift.thrift.api.tutorial.Calculator$Iface
'''

### Riferimenti per lo sviluppo
Mojo-Executor, per eseguire un plugin da un plugin:
https://github.com/TimMoore/mojo-executor
pechè il plugin chiamata mojo
thrift-plugin il cui sorgente che ho installato nella mia .m2 e che ho preso da
https://github.com/apache/thrift/tree/master/contrib/thrift-maven-plugin
è nella directory thrifplugin del progetto
per compilarlo basta fare mvn install
dopodichè andrà messo su nexus con mvn deploy ...
Ho messo skipTest a true nel pom perchè i test hanno problemi forse mancano delle configurazioni.

Per i test ho usato il plugin.
http://maven.apache.org/plugin-testing/maven-plugin-testing-harness/

http://thrift.apache.org/



Ultimo erore:
Caused by: com.sun.xml.bind.v2.runtime.IllegalAnnotationsException: 2 counts of IllegalAnnotationExceptions
Class has two properties of the same name "key"
	this problem is related to the following location:
		at public int drift.drift.thrift.api.shared.SharedStruct.getKey()
		at drift.drift.thrift.api.shared.SharedStruct
	this problem is related to the following location:
		at public int drift.drift.thrift.api.shared.SharedStruct.key
		at drift.drift.thrift.api.shared.SharedStruct
Class has two properties of the same name "value"
	this problem is related to the following location:
		at public java.lang.String drift.drift.thrift.api.shared.SharedStruct.getValue()
		at drift.drift.thrift.api.shared.SharedStruct
	this problem is related to the following location:
		at public java.lang.String drift.drift.thrift.api.shared.SharedStruct.value
		at drift.drift.thrift.api.shared.SharedStruct

	at com.sun.xml.bind.v2.runtime.IllegalAnnotationsException$Builder.check(IllegalAnnotationsException.java:106)
	at com.sun.xml.bind.v2.runtime.JAXBContextImpl.getTypeInfoSet(JAXBContextImpl.java:460)
	at com.sun.xml.bind.v2.runtime.JAXBContextImpl.<init>(JAXBContextImpl.java:292)
	at com.sun.xml.bind.v2.runtime.JAXBContextImpl.<init>(JAXBContextImpl.java:139)
	at com.sun.xml.bind.v2.runtime.JAXBContextImpl$JAXBContextBuilder.build(JAXBContextImpl.java:1138)
	at com.sun.xml.bind.v2.ContextFactory.createContext(ContextFactory.java:162)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:497)
	at javax.xml.bind.ContextFinder.newInstance(ContextFinder.java:247)
	at javax.xml.bind.ContextFinder.newInstance(ContextFinder.java:234)
	at javax.xml.bind.ContextFinder.find(ContextFinder.java:441)
	at javax.xml.bind.JAXBContext.newInstance(JAXBContext.java:641)
	at org.apache.cxf.common.jaxb.JAXBContextCache$2.run(JAXBContextCache.java:347)
	at org.apache.cxf.common.jaxb.JAXBContextCache$2.run(JAXBContextCache.java:345)
	at java.security.AccessController.doPrivileged(Native Method)
	at org.apache.cxf.common.jaxb.JAXBContextCache.createContext(JAXBContextCache.java:345)
	at org.apache.cxf.common.jaxb.JAXBContextCache.getCachedContextAndSchemas(JAXBContextCache.java:246)
	at org.apache.cxf.jaxb.JAXBDataBinding.createJAXBContextAndSchemas(JAXBDataBinding.java:474)
	at org.apache.cxf.jaxb.JAXBDataBinding.initialize(JAXBDataBinding.java:329)
	... 40 more
