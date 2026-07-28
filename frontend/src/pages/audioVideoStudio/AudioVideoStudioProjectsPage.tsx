import { Link } from "react-router-dom";
import { PlusCircle } from "lucide-react";
import { useVideoProjects } from "../../api/salesVideo/useVideoProjects";
import PageTitle from "../../components/PageTitle";
import "./AudioVideoStudioPage.css";

type ProjectListItem = {
  id: number | "EXEMPLO";
  title: string;
  objective: string;
  status: string;
  targetChannel: string;
  format: string;
  createdAt?: string | null;
  isExample?: boolean;
};

const exampleProject: ProjectListItem = {
  id: "EXEMPLO",
  title: "MUSA PDE v6 - video HLS motivacional",
  objective:
    "Projeto de referencia para localizar e revisar o video hero HLS do PDE v6 antes de evoluir roteiro, cenas, audio e revisao comercial.",
  status: "EXEMPLO",
  targetChannel: "PDE v6",
  format: "HLS LANDING_HERO",
  createdAt: null,
  isExample: true,
};

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
  const persistedProjects = videoProjectsQuery.data ?? [];
  const projects: ProjectListItem[] =
    persistedProjects.length > 0 ? persistedProjects : [exampleProject];

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
        ) : (
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
                      <strong>
                        {project.isExample ? project.id : `#${project.id}`}
                      </strong>
                      <span>{project.title}</span>
                      <small>{project.objective}</small>
                      {project.isExample ? (
                        <small>
                          HLS: /assets/hls/musa-v6-microexperiencia-visivel/index.m3u8
                        </small>
                      ) : null}
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
        )}
      </section>
    </div>
  );
}
