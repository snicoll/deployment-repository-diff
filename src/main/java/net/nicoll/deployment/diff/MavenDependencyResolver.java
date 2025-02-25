package net.nicoll.deployment.diff;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.maxxq.maven.dependency.DefaultDependencyFilter;
import org.maxxq.maven.dependency.GAV;
import org.maxxq.maven.dependency.ResolveDependencies;
import org.maxxq.maven.repository.IRepository;
import org.maxxq.maven.repository.LocalFileRepository;
import org.maxxq.maven.repository.RemoteRepository;
import org.maxxq.maven.repository.VirtualRepository;

public class MavenDependencyResolver {

	private final ResolveDependencies dependencyResolver;

	public MavenDependencyResolver(Path localRepositoryLocation) {
		IRepository repository = new VirtualRepository().addRepository(new LocalFileRepository(localRepositoryLocation))
			.addRepository(new RemoteRepository("https://repo1.maven.org/maven2"));
		this.dependencyResolver = new ResolveDependencies(repository);
		this.dependencyResolver.setDependenyFilter(new DependencyFilter());
	}

	public List<Dependency> resolveDependencies(String groupId, String artifactId, String version) {
		return new ArrayList<>(this.dependencyResolver.getDependencies(new GAV(groupId, artifactId, version)));
	}

	private static class DependencyFilter extends DefaultDependencyFilter {

		private static final List<String> SCOPES = List.of("compile", "runtime");

		@Override
		public boolean keepDependency(Dependency dependency, int depth) {
			String scope = dependency.getScope();
			return scope != null && SCOPES.contains(scope);
		}

	}

}
