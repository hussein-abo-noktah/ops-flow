import {useState} from 'react';
import type {IncidentComment} from '@/modules/incidents/types/incident.types.ts';
import {useIncidentComments} from '@/modules/incidents/hooks/useIncident.ts';
import {IncidentCommentEditor} from '@/modules/incidents/component/IncidentCommentEditor.tsx';
import {IncidentHistory, IncidentSection, IncidentTimestamp} from '@/modules/incidents/component/IncidentDetailUI.tsx';

export function IncidentComments({incidentId, userId, canComment, canEdit}: {
    incidentId: string; userId?: string; canComment: boolean; canEdit: (comment: IncidentComment) => boolean;
}) {
    const [page, setPage] = useState(0);
    const query = useIncidentComments(incidentId, page);
    const [editing, setEditing] = useState<IncidentComment | null>(null);
    return <IncidentSection id="incident-comments" title="Comments">
        {canComment && <IncidentCommentEditor incidentId={incidentId} />}
        <IncidentHistory query={query} label="comments" onPageChange={next => {setPage(next); setEditing(null);}}>{comments => <ul className="incident-comments">
            {comments.map(comment => <li key={comment.id}>
                <div className="incident-comment-meta"><strong>{comment.userId === userId ? 'You' : comment.userId}</strong><IncidentTimestamp value={comment.createdAt} />{comment.edited && <span>Edited · version {comment.version}</span>}</div>
                {editing?.id === comment.id && canEdit(comment)
                    ? <IncidentCommentEditor key={comment.id} incidentId={incidentId} existing={editing} latest={comment} onDone={() => setEditing(null)} />
                    : <><p className="incident-detail-text">{comment.comment}</p>{canEdit(comment) && <button className="incident-button" onClick={() => setEditing(comment)}>Edit comment</button>}</>}
            </li>)}
        </ul>}</IncidentHistory>
    </IncidentSection>;
}
