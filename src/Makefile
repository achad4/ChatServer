JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	User.java \
	InitServer.java \
	Server.java \
	InitClient.java \
	Client.java \
	Message.java \
	UserSession.java \


default: classes

classes: $(CLASSES:.java=.class)
clean:
	$(RM) *.class