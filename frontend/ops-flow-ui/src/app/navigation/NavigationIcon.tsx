import type {NavigationIconName} from '@/app/navigation/type.ts';

const paths: Record<NavigationIconName, string> = {
    overview: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
    incidents: 'M12 3 2 21h20L12 3Z M12 9v5 M12 17v.1',
    teams: 'M3 21v-8h7v8 M14 21V3h7v18 M6 16h1 M17 7h1 M17 11h1 M17 15h1 M1 21h22',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M17 4a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87',
    audit: 'M14 2H5v20h14V7l-5-5Z M14 2v6h5 M8 12h8 M8 16h8',
};

export function NavigationIcon({name}: {name: NavigationIconName}) {
    return (
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            focusable="false"
        >
            <path d={paths[name]} />
        </svg>
    );
}
