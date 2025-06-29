import { useFacebookAccounts } from '../api/useFacebookAccounts';

export default function AccountsPage() {
  const { data, isLoading, error } = useFacebookAccounts();

  if (isLoading) return <p>Loading...</p>;
  if (error) return <p>Error loading accounts</p>;

  return (
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Currency</th>
        </tr>
      </thead>
      <tbody>
        {data?.map(({ id, name, currency }) => (
          <tr key={id}>
            <td>{id}</td>
            <td>{name}</td>
            <td>{currency}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
