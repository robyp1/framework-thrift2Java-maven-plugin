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



$ git remote add origin file:///share/git/project-X
-->file://cad1652/generali_dev/gitrepo

# We push to the remote repository
$ git push origin master

