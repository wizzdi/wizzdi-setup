#!/bin/bash
get_host_name() {
 /bin/cat /home/flexicore/flexicore.config | /bin/grep iOTExternalId | /usr/bin/awk -F"="  '{print $2}'
}
get_cloud_url() {
	/bin/grep -Po '"basePathApi":.*?[^\\]",' /home/flexicore/remoteServer.json | /usr/bin/awk -F'":"'  '{print $2}'| /usr/bin/awk -F'"'  '{print $1}'

}



URL="https://hooks.slack.com/services/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" # Slack Webhook URL
HOST=$(get_host_name)
CLOUD=$(get_cloud_url)
PAYLOAD="{
  \"attachments\": [
    {
      \"title\": \"$PROCESS was restarted\",
      \"color\": \"warning\",
      \"mrkdwn_in\": [\"text\"],
      \"fields\": [
        { \"title\": \"Date\", \"value\": \"$MONIT_DATE\", \"short\": true },
        { \"title\": \"Endpoint\", \"value\": \"$HOST\", \"short\": true },
		{ \"title\": \"Related Cloud\", \"value\": \"$CLOUD\", \"short\": true }
      ]
    }
  ]
}"

/usr/bin/curl -s -X POST --data-urlencode "payload=$PAYLOAD" $URL

echo $PAYLOAD
