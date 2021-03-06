while getopts "apbcilr" option; do
  case $option in
  a) all=true ;;
  b) build=true ;;
  p) pull=true ;;
  c) crawl=true ;;
  i) index=true ;;
  l) lib=true ;;
  r) resume=true ;;
  esac
done
cd ~/Search_Engine
if [ "$all" == true ] || [ "$pull" == true ]; then
  git pull
fi
if [ "$all" == true ] || [ "$build" == true ] || [ "$pull" == true ]; then
  find . -name "*.java" -print0 | xargs -0 javac -cp "lib/*":"lib/mongo-driver-3.6.3/*"
  sudo rm -rf /opt/tomcat/webapps/ROOT/WEB-INF/classes/main
  sudo cp -r src/main/ /opt/tomcat/webapps/ROOT/WEB-INF/classes
  sudo cp data/web.xml /opt/tomcat/webapps/ROOT/WEB-INF/
  sudo cp data/*.txt /opt/tomcat/data/
fi

# Copy libs
if [ "$all" == true ] || [ "$lib" == true ]; then
  sudo rm -rf /opt/tomcat/webapps/ROOT/WEB-INF/lib/*
  sudo cp lib/*.jar /opt/tomcat/webapps/ROOT/WEB-INF/lib/
  sudo cp lib/mongo-driver-3.6.3/*.jar /opt/tomcat/webapps/ROOT/WEB-INF/lib/
fi

if [ "$all" == true ] || [ "$crawl" == true ]; then
  if [ "$resume" != true ]; then
    mongo search_engine --eval "db.crawler_info.drop()"
    sudo rm -rf /opt/tomcat/data/pages/*
  fi
  cd /opt/tomcat/
  java -cp webapps/ROOT/WEB-INF/classes:"webapps/ROOT/WEB-INF/lib/*" main.crawler.WebCrawler
fi
if [ "$all" == true ] || [ "$index" == true ]; then
  if [ "$resume" != true ]; then
    mongo search_engine --eval "db.invertedIndex.drop(); db.forwardIndex.drop(); db.imagesIndex.drop(); db.crawler_info.updateMany({}, {\$set: {indexed: false}})"
  fi
  cd /opt/tomcat/
  java -cp webapps/ROOT/WEB-INF/classes:"webapps/ROOT/WEB-INF/lib/*" main.indexer.Indexer
fi
sudo ufw allow 8080
sudo systemctl restart tomcat
