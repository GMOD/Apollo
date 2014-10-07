


curl -b cookies.txt -c cookies.txt -e  "http://localhost:8080" -H "Content-Type:application/json" -d "{'username': 'admin2', 'password': 'password2'}" "http://localhost:8080/apollo/Login?operation=login"



curl -b cookies.txt -c cookies.txt -e  "http://localhost:8080" --data '{ "track": "Annotations-Group1.1", "features": [{"location":{"fmin":559153,"fmax":559540,"strand":1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"GB42178-RA","children":[{"location":{"fmin":559153,"fmax":559540,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}}]}], "operation": "add_transcript" }' http://localhost:8080/apollo/AnnotationEditorService


