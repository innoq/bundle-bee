@echo off
echo Starting BundleBee
rem ${pom.version}

java -jar plugins/org.eclipse.equinox.launcher_1.0.101.R34x_v20081125.jar -console -noExit -configuration plugins

PAUSE
