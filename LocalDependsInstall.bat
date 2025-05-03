@echo off
echo Run the project dependency installation script
git clone https://github.com/QuiltServerTools/Ledger.git
cd Ledger
echo Cloning of dependency repositories completed
gradlew publishToMavenLocal
echo Installation successful
pause