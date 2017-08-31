#!/usr/bin/env groovy
import hudson.scm.ChangeLogSet;

/**
 * Send notifications based on build status string
 *
 * We support one non-standard string:
 *    ARCHIVE_FAILED
 */
@NonCPS
def call(String buildStatus = 'STARTED', List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSet) {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def color = 'RED'
  def colorCode = '#E74C3C'

  def changeString
  def print_changes = false

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
    buildStatus = 'Started'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#27AE60'
    buildStatus = 'Successful'
    print_changes = true
  } else if (buildStatus == 'ABORTED') {
    color = 'GREY'
    colorCode = '#D7DBDD'
    buildStatus = 'Aborted'
  } else if (buildStatus == 'ARCHIVE_FAILED') {
    // Use default colors
    buildStatus = 'Archive failed'
  } else if (buildStatus == 'FAILED') {
    print_changes = true
  }

  if(print_changes)
  {
    changeString = "Changes:\n" + getChangeString(changeSet)
  }

  // Slack
  def slack_msg = "${env.JOB_NAME} #${env.BUILD_NUMBER}:\nStatus: ${buildStatus} (<${env.BUILD_URL}|Open>)\n" + changeString
  slackSend (color: colorCode, message: slack_msg)

  //Email
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def details = """<p>${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

/* TODO
  emailext (
      to: 'bitwiseman@bitwiseman.com',
      subject: subject,
      body: details,
      recipientProviders: [[$class: 'DevelopersRecipientProvider']]
    )
*/
}

@NonCPS
def getChangeString(List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeLogSets) {
  MAX_MSG_LEN = 100
  def changeString = ""

  echo "Gathering SCM changes"

  for (int i = 0; i < changeLogSets.size(); i++) {
    def entries = changeLogSets[i].items
    for (int j = 0; j < entries.length; j++) {
      def entry = entries[j]
      truncated_msg = entry.msg.take(MAX_MSG_LEN)
      changeString += " - ${truncated_msg} [${entry.author}]\n"
    }
  }

  if (!changeString) {
    changeString = " - No new changes"
  }
  return changeString
}