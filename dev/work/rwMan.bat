set RWM_DEPS=C:\dev\jakarta-repo-pub\jakartaee-prototype\dev\transformer\build\archives
set RWM_LIBS=C:\dev\jakarta-repo-pub\jakartaee-prototype\dev\transformer\build\libs

set RWM_DEP_LIBS=%RWM_DEPS%\biz.aQute.bndlib-4.3.1.jar;%RWM_DEPS%\commons-cli-1.4.jar;%RWM_DEPS%\slf4j-api-1.7.29.jar;%RWM_DEPS%\slf4j-simple-1.7.29.jar
set RWM_LIB=%RWM_LIBS%\transformer.jar

set RWM_CP=%RWM_DEP_LIBS%;%RWM_LIB%
set RWM_CLASS=com.ibm.ws.jakarta.transformer.util.ManifestWriter

set RWM_CMD=java -cp %RWM_CP% %RWM_CLASS%

%RWM_CMD% %*
