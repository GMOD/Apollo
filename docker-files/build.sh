#!/usr/bin/env bash
cd /apollo/ && \
	# note that clean-all does more than remove the target directory files (which it does not do in its entirety)
    ./apollo clean-all && rm -rf target/* && ./apollo deploy && \
    cp /apollo/target/apollo*.war /tmp/apollo.war && \
	# here we save the tools directory
	# So we can remove ~1.6 GB of cruft from the image. Ignore errors because cannot remove parent dir /apollo/
    rm -rf /apollo/ || true && \
	# Before moving back into a standardized location (that we have write access to)
	mv /tmp/apollo.war /apollo/apollo.war
