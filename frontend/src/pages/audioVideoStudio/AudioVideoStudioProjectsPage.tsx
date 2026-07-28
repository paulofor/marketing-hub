import { Link } from "react-router-dom";
import { PlusCircle } from "lucide-react";
import { useVideoProjects } from "../../api/salesVideo/useVideoProjects";
import PageTitle from "../../components/PageTitle";
import "./AudioVideoStudioPage.css";

function formatDate(value?: string | null) {
  if (!value) {
    return "Sem data";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export default function AudioVideoStudioProjectsPage() {
  const videoProjectsQuery = useVideoProjects();
  const projects = videoProjectsQuery.data ?? [];

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title="Lista de projetos"
        subtitle="Projetos ja criados no Estudio de Audio e Video para organizar roteiro, cenas, audio, montagem e revisao comercial."
      />

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading audio-video-studio-page__section-heading--actions">
          <div>
            <h2>Projetos do estudio</h2>
            <p>
              Use esta lista para encontrar os projetos persistidos e decidir o
              proximo passo de producao.
            </p>
          </div>
          <Link
            className="audio-video-studio-page__secondary-action"
            to="/audio-video-studio"
          >
            <PlusCircle size={18} aria-hidden="true" />
            Novo projeto
          </Link>
        </div>

        {videoProjectsQuery.isLoading ? (
          <article className="audio-video-studio-page__project-card">
            Carregando projetos...
          </article>
        ) : videoProjectsQuery.isError ? (
          <article className="audio-video-studio-page__project-card">
            Nao foi possivel carregar os projetos agora.
          </article>
        ) : projects.length > 0 ? (
          <div className="audio-video-studio-page__project-table-wrapper">
            <table className="audio-video-studio-page__project-table">
              <thead>
                <tr>
                  <th>Projeto</th>
                  <th>Status</th>
                  <th>Canal</th>
                  <th>Formato</th>
                  <th>Criado em</th>
                </tr>
              </thead>
              <tbody>
                {projects.map((project) => (
                  <tr key={project.id}>
                    <td>
                      <strong>#{project.id}</strong>
                      <span>{project.title}</span>
                      <small>{project.objective}</small>
                    </td>
                    <td>{project.status}</td>
                    <td>{project.targetChannel}</td>
                    <td>{project.format}</td>
                    <td>{formatDate(project.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <article className="audio-video-studio-page__project-card">
            Nenhum projeto criado ainda. Crie o primeiro projeto no Estudio de
            Audio e Video.
          </article>
        )}
      </section>
    </div>
  );
}
