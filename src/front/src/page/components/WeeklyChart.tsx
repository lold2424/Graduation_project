import React from 'react';
import './WeeklyChart.css'; // 스타일 파일

// WeeklyChart.tsx

interface WeeklyChartProps {
    top10WeeklySongs: {
        id: string;
        title: string;
        artist: string;
        videoId: string;
    }[];
    title: string; // 제목을 props로 받도록 추가
}

const WeeklyChart: React.FC<WeeklyChartProps> = ({ top10WeeklySongs, title }) => {
    const handleSongClick = (videoId: string) => {
        const youtubeUrl = `https://www.youtube.com/watch?v=${videoId}`;
        window.open(youtubeUrl, '_blank');
    };

    return (
        <div className="weekly-chart">
            <h3>{title}</h3> {/* props로 전달된 제목을 사용 */}
            <ul className="chart-list">
                {top10WeeklySongs.length > 0 ? (
                    top10WeeklySongs.map((song, index) => (
                        <li key={song.id} className="chart-item" onClick={() => handleSongClick(song.videoId)} style={{ cursor: 'pointer', color: 'black' }}>
                            <h4>{index + 1}. {song.title}</h4>
                            <span>{song.artist}</span>
                        </li>
                    ))
                ) : (
                    <p>차트가 없습니다.</p>
                )}
            </ul>
            <div className="chart-footer">월요일 00시 기준</div>
        </div>
    );
};

export default WeeklyChart;
