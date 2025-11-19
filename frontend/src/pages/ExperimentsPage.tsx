
import { ExperimentsList } from '../components/ExperimentsList';
import { fetchExperiments } from '../services/experiments';

export function ExperimentsPage() {
  const { data: experiments, isLoading } = useQuery(['experiments'], fetchExperiments);

  const sortedExperiments = experiments?.toSorted((a, b) =>
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );

  if (isLoading) {
    return <div>Loading...</div>;
  }

  return <ExperimentsList experiments={sortedExperiments ?? []} />;
}
