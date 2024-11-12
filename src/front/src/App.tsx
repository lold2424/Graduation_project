import React, { useState, useEffect } from 'react';
import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './page/components/Header';
import MainPage from './page/MainPage';
import SearchResultsPage from './page/SearchResultsPage';
import WeeklyChart from './page/components/WeeklyChart';
import axios from 'axios';

const apiUrl = process.env.REACT_APP_API_URL;

const App: React.FC = () => {
    const [top10WeeklySongs, setTop10WeeklySongs] = useState<any[]>([]);
    const [top10DailySongs, setTop10DailySongs] = useState<any[]>([]);
    const [top10WeeklyShorts, setTop10WeeklyShorts] = useState<any[]>([]);  // 주간 쇼츠 데이터 추가
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [chartType, setChartType] = useState<'weekly' | 'daily' | 'shorts'>('weekly'); // chartType 상태를 'weekly'로 초기화

    useEffect(() => {
        axios.get(`http://localhost:8080/main`)
            .then((response) => {
                setTop10WeeklySongs(response.data.top10WeeklySongs || []);
                setTop10DailySongs(response.data.top10DailySongs || []);
                setTop10WeeklyShorts(response.data.top10WeeklyShorts || []); // 주간 인기 쇼츠 데이터 설정
                setIsLoading(false);
            })
            .catch((error) => {
                console.error('차트 데이터를 가져오는 중 오류 발생:', error);
                setError('차트 데이터를 불러오는 중 오류가 발생했습니다.');
                setIsLoading(false);
            });
    }, []);

    return (
        <Router>
            <div className="app-container">
                <Header />
                <div className="main-layout">
                    <div className="content">
                        <Routes>
                            <Route path="/" element={<MainPage />} />
                            <Route path="/search" element={<SearchResultsPage />} />
                        </Routes>
                    </div>
                    <div className="sidebar">
                        <div className="button-group">
                            <button
                                onClick={() => setChartType('weekly')}
                                className={chartType === 'weekly' ? 'active' : ''}
                            >
                                주간
                            </button>
                            <button
                                onClick={() => setChartType('daily')}
                                className={chartType === 'daily' ? 'active' : ''}
                            >
                                일간
                            </button>
                            <button
                                onClick={() => setChartType('shorts')}
                                className={chartType === 'shorts' ? 'active' : ''}
                            >
                                쇼츠
                            </button>
                        </div>

                        {isLoading ? (
                            <p>차트를 불러오는 중입니다...</p>
                        ) : error ? (
                            <p>{error}</p>
                        ) : (
                            <>
                                {chartType === 'weekly' && (
                                    <WeeklyChart top10WeeklySongs={top10WeeklySongs} title="주간 인기 노래" />
                                )}
                                {chartType === 'daily' && (
                                    <WeeklyChart top10WeeklySongs={top10DailySongs} title="일간 인기 노래" />
                                )}
                                {chartType === 'shorts' && (
                                    <WeeklyChart top10WeeklySongs={top10WeeklyShorts} title="주간 인기 쇼츠" />
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>
        </Router>
    );
};

export default App;
