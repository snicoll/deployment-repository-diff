/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nicoll.deployment.diff;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

@SuppressWarnings("deprecation")
class MavenDependencyResolver {

	private static final RemoteRepository mavenCentral = new RemoteRepository.Builder("central", "default",
			"https://repo1.maven.org/maven2")
		.build();

	private static final RemoteRepository springMilestones = new RemoteRepository.Builder("spring-milestones",
			"default", "https://repo.spring.io/milestone")
		.build();

	private static final RemoteRepository springSnapshots = new RemoteRepository.Builder("spring-snapshots", "default",
			"https://repo.spring.io/snapshot")
		.build();

	private static final List<RemoteRepository> repositories = Arrays.asList(mavenCentral, springMilestones,
			springSnapshots);

	private final Object monitor = new Object();

	private final RepositorySystemSession repositorySystemSession;

	private final RepositorySystem repositorySystem;

	MavenDependencyResolver(Path localRepositoryLocation) {
		ServiceLocator serviceLocator = createServiceLocator();
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(false, false));
		LocalRepository localRepository = new LocalRepository(localRepositoryLocation.toFile());
		this.repositorySystem = serviceLocator.getService(RepositorySystem.class);
		session.setLocalRepositoryManager(this.repositorySystem.newLocalRepositoryManager(session, localRepository));
		session.setUserProperties(System.getProperties());
		session.setReadOnly();
		this.repositorySystemSession = session;
	}

	public List<Dependency> resolveDependencies(String groupId, String artifactId, String version) {
		ArtifactDescriptorResult pom = resolvePom(groupId, artifactId, version);
		return pom.getDependencies();
	}

	private ArtifactDescriptorResult resolvePom(String groupId, String artifactId, String version) {
		synchronized (this.monitor) {
			try {
				return this.repositorySystem.readArtifactDescriptor(this.repositorySystemSession,
						new ArtifactDescriptorRequest(new DefaultArtifact(groupId, artifactId, "pom", version),
								repositories, null));
			}
			catch (ArtifactDescriptorException ex) {
				throw new IllegalStateException(
						"Pom '" + groupId + ":" + artifactId + ":" + version + "' could not be resolved", ex);
			}
		}
	}

	private static ServiceLocator createServiceLocator() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositorySystem.class, DefaultRepositorySystem.class);
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		return locator;
	}

}
