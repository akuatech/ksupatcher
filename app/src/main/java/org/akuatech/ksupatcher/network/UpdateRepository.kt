package org.akuatech.ksupatcher.network

class UpdateRepository(
    private val releaseRepository: GitHubReleaseRepository = GitHubReleaseRepository()
) {
    suspend fun fetchAppUpdateInfo(owner: String, repo: String, currentBuildHash: String) =
        releaseRepository.fetchAppUpdateInfo(owner, repo, currentBuildHash)
}
