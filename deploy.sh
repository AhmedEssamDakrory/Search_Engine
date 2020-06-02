while getopts "apcil" option; do
  case $option in
  a) all=true ;;
  p) pull=true ;;
  c) crawl=true ;;
  i) index=true ;;
  l) lib=true ;;
  esac
done
cd ~/Search_Engine
if [ "$all" == true ] || [ "$pull" == true ]; then
  git pull
fi
# Compile
javac -cp "lib/*":"lib/mongo-driver-3.6.3/*" src/main/*/*.java
sudo rm -rf /opt/tomcat/webapps/ROOT/WEB-INF/classes/main
sudo cp -r src/main/ /opt/tomcat/webapps/ROOT/WEB-INF/classes
sudo cp -r data/web.xml /opt/tomcat/webapps/ROOT/WEB-INF/

# Copy libs
if [ "$all" == true ] || [ "$lib" == true ]; then
  sudo rm -rf /opt/tomcat/webapps/ROOT/WEB-INF/lib/*
  sudo cp lib/*.jar /opt/tomcat/webapps/ROOT/WEB-INF/lib/
  sudo cp lib/mongo-driver-3.6.3/*.jar /opt/tomcat/webapps/ROOT/WEB-INF/lib/
fi

if [ "$all" == true ] || [ "$crawl" == true ]; then
  cd /opt/tomcat/webapps/ROOT/WEB-INF/classes
  java -cp .:"../lib/*" main/crawler/WebCrawler
fi
if [ "$all" == true ] || [ "$index" == true ]; then
  mongo search_engine --eval "db.invertedIndex.drop(); db.forwardIndex.drop(); db.imagesIndex.drop()"
  cd /opt/tomcat/webapps/ROOT/WEB-INF/classes
  java -cp .:"../lib/*" main/indexer/Indexer
fi
sudo ufw allow 8080
sudo chown -R tomcat /opt/tomcat/
sudo systemctl restart tomcat
