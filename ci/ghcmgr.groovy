import groovy.json.JsonBuilder
/**
 * Methods for interacting with ghcmgr API.
 * For more details see:
 * https://github.com/status-im/github-comment-manager
 **/

def cmgrBuildObj(success) {
  /* assemble build object valid for ghcmgr */
  return [
    id: env.BUILD_DISPLAY_NAME,
    commit: GIT_COMMIT.take(8),
    success: success != null ? success : true,
    platform: env.BUILD_PLATFORM + (getBuildType() == 'e2e' ? '-e2e' : ''),
    duration: buildDuration(),
    url: currentBuild.absoluteUrl,
    pkg_url: env.PKG_URL,
  ]
}

def cmgrPostBuild(success) {
  /**
   * This is our own service for avoiding comment spam.
   * https://github.com/status-im/github-comment-manager
   **/
  def ghcmgrurl = 'https://ghcmgr.status.im'
  def changeId = changeId()
  def body = cmgrBuildObj(success)
  def json = new JsonBuilder(body).toPrettyString()
  withCredentials([usernamePassword(
    credentialsId:  'ghcmgr-auth',
    usernameVariable: 'GHCMGR_USER',
    passwordVariable: 'GHCMGR_PASS'
  )]) {
    sh """
      curl --silent --verbose -XPOST --data '${json}' \
        -u '${GHCMGR_USER}:${GHCMGR_PASS}' \
        -H "content-type: application/json" \
        '${ghcmgrurl}/builds/${changeId}'
    """
  }
}

return this
